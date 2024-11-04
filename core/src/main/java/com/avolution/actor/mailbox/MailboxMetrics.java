package com.avolution.actor.mailbox;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 邮箱性能指标收集器
 */
public class MailboxMetrics {
    // 基础计数器
    private final LongAdder messagesEnqueued = new LongAdder();
    private final LongAdder messagesProcessed = new LongAdder();
    private final LongAdder messagesFailed = new LongAdder();
    private final LongAdder messagesRejected = new LongAdder();
    private final LongAdder systemMessagesProcessed = new LongAdder();
    
    // 性能指标
    private final LongAdder processingTimeNanos = new LongAdder();
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong();
    private final LongAdder batchesProcessed = new LongAdder();
    
    // 邮箱状态计数
    private final LongAdder suspensionCount = new LongAdder();
    private final LongAdder resumeCount = new LongAdder();
    private final LongAdder clearCount = new LongAdder();
    private final LongAdder messagesCleared = new LongAdder();
    
    // 时间戳
    private final Instant startTime;
    private volatile Instant lastProcessedTime;
    private volatile Instant lastFailureTime;
    private volatile Instant lastSuspendedTime;
    private volatile Instant lastResumedTime;

    public MailboxMetrics() {
        this.startTime = Instant.now();
    }

    // 更新方法
    public void messageEnqueued() {
        messagesEnqueued.increment();
    }

    public void messageProcessed(long processingTimeNanos) {
        messagesProcessed.increment();
        this.processingTimeNanos.add(processingTimeNanos);
        maxProcessingTimeNanos.accumulateAndGet(processingTimeNanos, Math::max);
        lastProcessedTime = Instant.now();
    }

    public void systemMessageProcessed(long processingTimeNanos) {
        systemMessagesProcessed.increment();
        messageProcessed(processingTimeNanos);
    }

    public void messageFailure() {
        messagesFailed.increment();
        lastFailureTime = Instant.now();
    }

    public void messageRejected() {
        messagesRejected.increment();
    }

    public void mailboxSuspended() {
        suspensionCount.increment();
        lastSuspendedTime = Instant.now();
    }

    public void mailboxResumed() {
        resumeCount.increment();
        lastResumedTime = Instant.now();
    }

    public void messagesCleared(int count) {
        clearCount.increment();
        messagesCleared.add(count);
    }

    public void batchCompleted() {
        batchesProcessed.increment();
    }

    // 获取统计信息
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            messagesEnqueued.sum(),
            messagesProcessed.sum(),
            systemMessagesProcessed.sum(),
            messagesFailed.sum(),
            messagesRejected.sum(),
            getAverageProcessingTimeMs(),
            getMaxProcessingTimeMs(),
            getMessagesPerSecond(),
            getFailureRate(),
            suspensionCount.sum(),
            resumeCount.sum(),
            clearCount.sum(),
            messagesCleared.sum(),
            getUptime(),
            lastProcessedTime,
            lastFailureTime,
            lastSuspendedTime,
            lastResumedTime
        );
    }

    // 计算派生指标
    private double getAverageProcessingTimeMs() {
        long processed = messagesProcessed.sum();
        return processed > 0 ? 
            (processingTimeNanos.sum() / processed) / 1_000_000.0 : 0.0;
    }

    private double getMaxProcessingTimeMs() {
        return maxProcessingTimeNanos.get() / 1_000_000.0;
    }

    private double getMessagesPerSecond() {
        double uptime = getUptime().toSeconds();
        return uptime > 0 ? messagesProcessed.sum() / uptime : 0.0;
    }

    private double getFailureRate() {
        long total = messagesProcessed.sum() + messagesFailed.sum();
        return total > 0 ? (double) messagesFailed.sum() / total : 0.0;
    }

    private Duration getUptime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * 不可变的度量快照
     */
    public record MetricsSnapshot(
        long messagesEnqueued,
        long messagesProcessed,
        long systemMessagesProcessed,
        long messagesFailed,
        long messagesRejected,
        double averageProcessingTimeMs,
        double maxProcessingTimeMs,
        double messagesPerSecond,
        double failureRate,
        long suspensionCount,
        long resumeCount,
        long clearCount,
        long messagesCleared,
        Duration uptime,
        Instant lastProcessedTime,
        Instant lastFailureTime,
        Instant lastSuspendedTime,
        Instant lastResumedTime
    ) {
        @Override
        public String toString() {
            return String.format("""
                Mailbox Metrics:
                Messages: enqueued=%d, processed=%d, failed=%d, rejected=%d
                System Messages: processed=%d
                Performance: avg=%.2fms, max=%.2fms, throughput=%.2f msg/s
                Failure Rate: %.2f%%
                Status Changes: suspensions=%d, resumes=%d, clears=%d (total cleared=%d)
                Uptime: %s
                Last Events: processed=%s, failed=%s, suspended=%s, resumed=%s
                """,
                messagesEnqueued, messagesProcessed, messagesFailed, messagesRejected,
                systemMessagesProcessed,
                averageProcessingTimeMs, maxProcessingTimeMs, messagesPerSecond,
                failureRate * 100,
                suspensionCount, resumeCount, clearCount, messagesCleared,
                uptime,
                lastProcessedTime, lastFailureTime, lastSuspendedTime, lastResumedTime
            );
        }
    }
}