package com.avolution.actor.mailbox;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮箱性能指标收集类
 * 
 * 该类负责收集和管理Actor邮箱的各项性能指标，包括：
 * - 消息统计（总数、系统消息、普通消息等）
 * - 处理时间统计
 * - 错误和拒绝统计
 * - 状态变更统计
 * 
 * 使用示例：
 * <pre>
 * {@code
 * MailboxMetrics metrics = new MailboxMetrics();
 * 
 * // 记录消息
 * metrics.recordNormalMessage();
 * metrics.recordSystemMessage();
 * 
 * // 记录处理时间
 * long startTime = System.nanoTime();
 * // 处理消息
 * metrics.recordProcessingTime(System.nanoTime() - startTime);
 * 
 * // 获取统计快照
 * MetricsSnapshot snapshot = metrics.getSnapshot();
 * logger.info("Average processing time: {} ns", snapshot.getAverageProcessingTimeNanos());
 * }
 * </pre>
 * 
 * 注意：
 * 1. 所有计数器都是线程安全的
 * 2. 可以通过reset()方法重置所有计数器
 * 3. 建议定期获取快照进行监控
 */
public class MailboxMetrics {
    private static final Logger logger = LoggerFactory.getLogger(MailboxMetrics.class);

    private final AtomicLong totalMessages = new AtomicLong();
    private final AtomicLong systemMessages = new AtomicLong();
    private final AtomicLong normalMessages = new AtomicLong();
    private final AtomicLong rejectedMessages = new AtomicLong();
    private final AtomicLong overflowMessages = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong totalProcessingTime = new AtomicLong();
    private final AtomicLong successfulEnqueues = new AtomicLong();
    private final AtomicLong systemMessagesProcessed = new AtomicLong();
    private final AtomicLong normalMessagesProcessed = new AtomicLong();
    private final AtomicLong stashedMessages = new AtomicLong();
    private final AtomicLong unstashedMessages = new AtomicLong();
    private final AtomicLong suspendCount = new AtomicLong();
    private final AtomicLong resumeCount = new AtomicLong();
    private final AtomicLong clearCount = new AtomicLong();
    private final AtomicLong closeCount = new AtomicLong();

    public void recordSystemMessage() {
        systemMessages.incrementAndGet();
        totalMessages.incrementAndGet();
        logger.trace("System message recorded");
    }

    public void recordNormalMessage() {
        normalMessages.incrementAndGet();
        totalMessages.incrementAndGet();
        logger.trace("Normal message recorded");
    }

    public void recordRejectedMessage() {
        rejectedMessages.incrementAndGet();
        logger.debug("Message rejected");
    }

    public void recordOverflowMessage() {
        overflowMessages.incrementAndGet();
        logger.debug("Message overflow occurred");
    }

    public void recordError() {
        errors.incrementAndGet();
        logger.debug("Error recorded");
    }

    public void recordProcessingTime(long nanos) {
        totalProcessingTime.addAndGet(nanos);
    }

    public void recordSuccessfulEnqueue() {
        successfulEnqueues.incrementAndGet();
    }

    public void recordSystemMessageProcessed() {
        systemMessagesProcessed.incrementAndGet();
    }

    public void recordNormalMessageProcessed() {
        normalMessagesProcessed.incrementAndGet();
    }

    public void recordStashedMessage() {
        stashedMessages.incrementAndGet();
    }

    public void recordUnstashedMessage() {
        unstashedMessages.incrementAndGet();
    }

    public void recordSuspend() {
        suspendCount.incrementAndGet();
        logger.debug("Mailbox suspended");
    }

    public void recordResume() {
        resumeCount.incrementAndGet();
        logger.debug("Mailbox resumed");
    }

    public void recordClear() {
        clearCount.incrementAndGet();
        logger.debug("Mailbox cleared");
    }

    public void recordClose() {
        closeCount.incrementAndGet();
        logger.debug("Mailbox closed");
    }

    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            totalMessages.get(),
            systemMessages.get(),
            normalMessages.get(),
            rejectedMessages.get(),
            overflowMessages.get(),
            errors.get(),
            totalProcessingTime.get(),
            successfulEnqueues.get(),
            systemMessagesProcessed.get(),
            normalMessagesProcessed.get(),
            stashedMessages.get(),
            unstashedMessages.get(),
            suspendCount.get(),
            resumeCount.get(),
            clearCount.get(),
            closeCount.get()
        );
    }

    public void reset() {
        totalMessages.set(0);
        systemMessages.set(0);
        normalMessages.set(0);
        rejectedMessages.set(0);
        overflowMessages.set(0);
        errors.set(0);
        totalProcessingTime.set(0);
        successfulEnqueues.set(0);
        systemMessagesProcessed.set(0);
        normalMessagesProcessed.set(0);
        stashedMessages.set(0);
        unstashedMessages.set(0);
        suspendCount.set(0);
        resumeCount.set(0);
        clearCount.set(0);
        closeCount.set(0);
        logger.info("Metrics reset");
    }
} 