package com.avolution.actor.mailbox;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 邮箱性能指标收集器
 */
public class MailboxMetrics {
    // 消息计数器
    private final LongAdder messagesEnqueued = new LongAdder();
    private final LongAdder messagesProcessed = new LongAdder();
    private final LongAdder messagesDropped = new LongAdder();
    private final LongAdder messagesRejected = new LongAdder();
    private final LongAdder messagesFailed = new LongAdder();
    private final LongAdder messagesOverflowed = new LongAdder();
    
    // 处理时间统计
    private final LongAdder processingTimeNanos = new LongAdder();
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong();
    private final LongAdder batchesProcessed = new LongAdder();
    
    // 时间戳
    private final Instant startTime;
    private volatile Instant lastProcessedTime;
    private volatile Instant lastFailureTime;

    public MailboxMetrics() {
        this.startTime = Instant.now();
    }

    // 更新方法
    public void messageEnqueued() {
        messagesEnqueued.increment();
    }

    public void messageProcessed() {
        messagesProcessed.increment();
        lastProcessedTime = Instant.now();
    }

    public void messageDropped() {
        messagesDropped.increment();
    }

    public void messageRejected() {
        messagesRejected.increment();
    }

    public void messageProcessingFailed() {
        messagesFailed.increment();
        lastFailureTime = Instant.now();
    }

    public void messageOverflowed() {
        messagesOverflowed.increment();
    }

    public void batchProcessed(long processingTimeNanos) {
        this.processingTimeNanos.add(processingTimeNanos);
        maxProcessingTimeNanos.accumulateAndGet(
            processingTimeNanos,
            Math::max
        );
        batchesProcessed.increment();
    }

    // 重置方法
    public void reset() {
        messagesEnqueued.reset();
        messagesProcessed.reset();
        messagesDropped.reset();
        messagesRejected.reset();
        messagesFailed.reset();
        messagesOverflowed.reset();
        processingTimeNanos.reset();
        maxProcessingTimeNanos.set(0);
        batchesProcessed.reset();
    }

    // 获取统计信息
    public long getMessagesEnqueued() {
        return messagesEnqueued.sum();
    }

    public long getMessagesProcessed() {
        return messagesProcessed.sum();
    }

    public long getMessagesDropped() {
        return messagesDropped.sum();
    }

    public long getMessagesRejected() {
        return messagesRejected.sum();
    }

    public long getMessagesFailed() {
        return messagesFailed.sum();
    }

    public long getMessagesOverflowed() {
        return messagesOverflowed.sum();
    }

    public long getBatchesProcessed() {
        return batchesProcessed.sum();
    }

    public Duration getUptime() {
        return Duration.between(startTime, Instant.now());
    }

    public Instant getLastProcessedTime() {
        return lastProcessedTime;
    }

    public Instant getLastFailureTime() {
        return lastFailureTime;
    }

    // 计算派生指标
    public double getAverageProcessingTimeMs() {
        long batches = batchesProcessed.sum();
        return batches > 0 
            ? (processingTimeNanos.sum() / batches) / 1_000_000.0 
            : 0.0;
    }

    public double getMaxProcessingTimeMs() {
        return maxProcessingTimeNanos.get() / 1_000_000.0;
    }

    public double getMessagesPerSecond() {
        double uptimeSeconds = getUptime().toSeconds();
        return uptimeSeconds > 0 
            ? messagesProcessed.sum() / uptimeSeconds 
            : 0.0;
    }

    public double getFailuresPerSecond() {
        double uptimeSeconds = getUptime().toSeconds();
        return uptimeSeconds > 0 
            ? messagesFailed.sum() / uptimeSeconds 
            : 0.0;
    }

    public double getDropRate() {
        long total = messagesEnqueued.sum();
        return total > 0 
            ? (double) messagesDropped.sum() / total 
            : 0.0;
    }

    public double getSuccessRate() {
        long total = messagesProcessed.sum() + messagesFailed.sum();
        return total > 0 
            ? (double) messagesProcessed.sum() / total 
            : 0.0;
    }

    // 获取快照
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            messagesEnqueued.sum(),
            messagesProcessed.sum(),
            messagesDropped.sum(),
            messagesRejected.sum(),
            messagesFailed.sum(),
            messagesOverflowed.sum(),
            getAverageProcessingTimeMs(),
            getMaxProcessingTimeMs(),
            getMessagesPerSecond(),
            getFailuresPerSecond(),
            getDropRate(),
            getSuccessRate(),
            getUptime(),
            lastProcessedTime,
            lastFailureTime
        );
    }

    /**
     * 不可变的度量快照
     */
    public record MetricsSnapshot(
        long messagesEnqueued,
        long messagesProcessed,
        long messagesDropped,
        long messagesRejected,
        long messagesFailed,
        long messagesOverflowed,
        double averageProcessingTimeMs,
        double maxProcessingTimeMs,
        double messagesPerSecond,
        double failuresPerSecond,
        double dropRate,
        double successRate,
        Duration uptime,
        Instant lastProcessedTime,
        Instant lastFailureTime
    ) {
        @Override
        public String toString() {
            return String.format("""
                Mailbox Metrics:
                Messages:
                  - Enqueued: %d
                  - Processed: %d
                  - Dropped: %d
                  - Rejected: %d
                  - Failed: %d
                  - Overflowed: %d
                Performance:
                  - Avg Processing Time: %.2f ms
                  - Max Processing Time: %.2f ms
                  - Throughput: %.2f msg/sec
                  - Failure Rate: %.2f failures/sec
                Rates:
                  - Drop Rate: %.2f%%
                  - Success Rate: %.2f%%
                Timing:
                  - Uptime: %s
                  - Last Processed: %s
                  - Last Failure: %s
                """,
                messagesEnqueued,
                messagesProcessed,
                messagesDropped,
                messagesRejected,
                messagesFailed,
                messagesOverflowed,
                averageProcessingTimeMs,
                maxProcessingTimeMs,
                messagesPerSecond,
                failuresPerSecond,
                dropRate * 100,
                successRate * 100,
                uptime,
                lastProcessedTime != null ? lastProcessedTime : "never",
                lastFailureTime != null ? lastFailureTime : "never"
            );
        }
    }
}