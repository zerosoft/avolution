package com.avolution.actor.mailbox;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.config.MailboxConfig;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Actor消息邮箱实现
 */
public class Mailbox {
    private final Queue<Envelope> queue;

    private final MailboxConfig config;
    private final MailboxMetrics metrics;

    private final AtomicBoolean suspended;
    private final AtomicBoolean processing;
    
    // 系统消息队列（优先级高于普通消息）
    private final Queue<Envelope> systemQueue;

    public Mailbox(MailboxConfig config) {
        this.config = config;
        this.queue = new ConcurrentLinkedQueue<>();
        this.systemQueue = new ConcurrentLinkedQueue<>();
        this.metrics = new MailboxMetrics();
        this.suspended = new AtomicBoolean(false);
        this.processing = new AtomicBoolean(false);
    }

    /**
     * 入队消息
     */
    public boolean enqueue(Envelope envelope) {
        if (suspended.get()) {
            metrics.messageRejected();
            return false;
        }

        Queue<Envelope> targetQueue = envelope.isSystemMessage() 
            ? systemQueue 
            : queue;

        if (targetQueue.size() >= config.getCapacity()) {
            handleOverflow(envelope);
            return false;
        }

        try {
            targetQueue.offer(envelope);
            metrics.messageEnqueued();
            return true;
        } catch (Exception e) {
            metrics.messageRejected();
            return false;
        }
    }

    /**
     * 处理队列中的消息
     */
    public void process(MessageProcessor processor) {
        if (suspended.get() || !processing.compareAndSet(false, true)) {
            return;
        }

        try {
            // 首先处理系统消息
            processQueue(systemQueue, processor);
            
            // 然后处理普通消息
            processQueue(queue, processor);
            
        } finally {
            processing.set(false);
        }
    }

    private void processQueue(Queue<Envelope> queue, MessageProcessor processor) {
        int processedCount = 0;
        long startTime = System.nanoTime();

        while (!suspended.get() && 
               processedCount < config.getBatchSize() && 
               !queue.isEmpty()) {
            
            Envelope envelope = queue.poll();
            if (envelope != null) {
                try {
                    processor.process(envelope);
                    metrics.messageProcessed();
                    processedCount++;
                } catch (Exception e) {
                    metrics.messageProcessingFailed();
                    handleProcessingError(envelope, e);
                }
            }
        }

        if (processedCount > 0) {
            metrics.batchProcessed(System.nanoTime() - startTime);
        }
    }

    /**
     * 暂停消息处理
     */
    public void suspend() {
        suspended.set(true);
    }

    /**
     * 恢复消息处理
     */
    public void resume() {
        suspended.set(false);
    }

    /**
     * 清空邮箱
     */
    public void clear() {
        queue.clear();
        systemQueue.clear();
        metrics.reset();
    }

    /**
     * 获取当前队列大小
     */
    public int size() {
        return queue.size() + systemQueue.size();
    }

    /**
     * 获取度量信息
     */
    public MailboxMetrics getMetrics() {
        return metrics;
    }

    private void handleOverflow(Envelope envelope) {
        metrics.messageOverflowed();
        switch (config.getOverflowStrategy()) {
            case DROP_NEW -> metrics.messageDropped();
            case DROP_OLD -> {
                queue.poll(); // 移除最老的消息
                enqueue(envelope); // 重试入队
            }
            case DROP_BUFFER -> {
                clear();
                enqueue(envelope);
            }
        }
    }

    private void handleProcessingError(Envelope envelope, Exception error) {
        if (config.getRetryStrategy().shouldRetry(envelope)) {
            enqueue(envelope); // 重试
        }
    }
} 