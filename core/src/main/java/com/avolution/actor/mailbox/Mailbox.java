package com.avolution.actor.mailbox;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageHandler;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Actor消息邮箱实现
 */
public class Mailbox {
    private final Queue<Envelope> queue;
    private final Queue<Envelope> systemQueue;
    private final MailboxMetrics metrics;

    private final AtomicBoolean suspended;
    private final AtomicBoolean processing;
    private final AtomicInteger unprocessedMessages;

    private final int throughput;
    private volatile boolean closed;

    public Mailbox() {
        this(100); // 默认吞吐量
    }

    public Mailbox(int throughput) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.systemQueue = new ConcurrentLinkedQueue<>();

        this.metrics = new MailboxMetrics();

        this.suspended = new AtomicBoolean(false);
        this.processing = new AtomicBoolean(false);

        this.unprocessedMessages = new AtomicInteger(0);
        this.throughput = throughput;
        this.closed = false;
    }

    /**
     * 入队消息
     */
    public boolean enqueue(Envelope envelope) {
        if (closed || suspended.get()) {
            metrics.messageRejected();
            return false;
        }

        boolean success;
        if (envelope.isSystemMessage()) {
            success = systemQueue.offer(envelope);
        } else {
            success = queue.offer(envelope);
        }

        if (success) {
            unprocessedMessages.incrementAndGet();
            metrics.messageEnqueued();
        }
        return success;
    }

    /**
     * 处理队列中的消息
     */
    public void process(MessageHandler handler) {
        if (processing.compareAndSet(false, true)) {
            try {
                int processed = 0;
                while (processed < throughput && !suspended.get()) {
                    // 优先处理系统消息
                    Envelope msg = systemQueue.poll();
                    if (msg == null) {
                        msg = queue.poll();
                    }
                    if (msg == null){
                        break;
                    }
                    try {
                        long nanoTime = System.nanoTime();
                        handler.handle(msg);
                        metrics.messageProcessed(System.nanoTime()-nanoTime);
                    } catch (Exception e) {
                        metrics.messageFailure();
//                        handler.handleFailure(msg, e);
                    } finally {
                        unprocessedMessages.decrementAndGet();
                        processed++;
                    }
                }
            } finally {
                processing.set(false);
            }
        }
    }

    /**
     * 暂停消息处理
     */
    public void suspend() {
        suspended.set(true);
        metrics.mailboxSuspended();
    }

    /**
     * 恢复消息处理
     */
    public void resume() {
        suspended.set(false);
        metrics.mailboxResumed();
    }

    /**
     * 清空邮箱
     */
    public void clear() {
        int cleared = unprocessedMessages.get();
        queue.clear();
        systemQueue.clear();
        unprocessedMessages.set(0);
        metrics.messagesCleared(cleared);
    }

    /**
     * 获取当前队列大小
     */
    public int size() {
        return unprocessedMessages.get();
    }

    /**
     * 获取度量信息
     */
    public MailboxMetrics getMetrics() {
        return metrics;
    }

    /**
     * 关闭邮箱
     */
    public void close() {
        closed = true;
        clear();
    }

    private void scheduleProcessing(MessageHandler handler) {
        CompletableFuture.runAsync(() -> process(handler));
    }

    public boolean hasMessages() {
        return !systemQueue.isEmpty() || !queue.isEmpty();
    }
}