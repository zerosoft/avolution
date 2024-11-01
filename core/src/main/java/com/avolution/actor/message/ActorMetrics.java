package com.avolution.actor.message;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Actor性能指标收集类
 */
public class ActorMetrics {
    // 消息统计
    private final LongAdder messagesProcessed;
    private final LongAdder messagesDropped;
    private final LongAdder messagesFailed;
    private final LongAdder mailboxSize;
    
    // 处理时间统计
    private final LongAdder processingTimeNanos;
    private final AtomicLong maxProcessingTimeNanos;
    
    // 错误统计
    private final LongAdder failuresCount;
    private final LongAdder restartCount;
    
    // 时间戳
    private final Instant startTime;
    private volatile Instant lastProcessedTime;
    private volatile Instant lastFailureTime;
    
    // 死信统计
    private final LongAdder deadLettersCount;
    
    public ActorMetrics() {
        this.messagesProcessed = new LongAdder();
        this.messagesDropped = new LongAdder();
        this.messagesFailed = new LongAdder();
        this.mailboxSize = new LongAdder();
        this.processingTimeNanos = new LongAdder();
        this.maxProcessingTimeNanos = new AtomicLong();
        this.failuresCount = new LongAdder();
        this.restartCount = new LongAdder();
        this.deadLettersCount = new LongAdder();
        this.startTime = Instant.now();
    }

    /**
     * 消息处理相关方法
     */
    public void messageReceived() {
        mailboxSize.increment();
    }

    public void messageProcessed(long processingTimeNanos) {
        messagesProcessed.increment();
        mailboxSize.decrement();
        this.processingTimeNanos.add(processingTimeNanos);
        updateMaxProcessingTime(processingTimeNanos);
        lastProcessedTime = Instant.now();
    }

    public void messageDropped() {
        messagesDropped.increment();
        mailboxSize.decrement();
    }

    public void messageFailed() {
        messagesFailed.increment();
        mailboxSize.decrement();
    }

    public void deadLetter() {
        deadLettersCount.increment();
    }

    /**
     * 错误处理相关方法
     */
    public void failureRecorded() {
        failuresCount.increment();
        lastFailureTime = Instant.now();
    }

    public void restartRecorded() {
        restartCount.increment();
    }

    /**
     * 获取统计数据
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            messagesProcessed.sum(),
            messagesDropped.sum(),
            messagesFailed.sum(),
            mailboxSize.sum(),
            getAverageProcessingTimeNanos(),
            maxProcessingTimeNanos.get(),
            failuresCount.sum(),
            restartCount.sum(),
            deadLettersCount.sum(),
            startTime,
            lastProcessedTime,
            lastFailureTime
        );
    }

    /**
     * 重置统计数据
     */
    public void reset() {
        messagesProcessed.reset();
        messagesDropped.reset();
        messagesFailed.reset();
        mailboxSize.reset();
        processingTimeNanos.reset();
        maxProcessingTimeNanos.set(0);
        failuresCount.reset();
        restartCount.reset();
        deadLettersCount.reset();
    }

    /**
     * 辅助方法
     */
    private void updateMaxProcessingTime(long processingTimeNanos) {
        long currentMax;
        do {
            currentMax = maxProcessingTimeNanos.get();
            if (processingTimeNanos <= currentMax) {
                break;
            }
        } while (!maxProcessingTimeNanos.compareAndSet(currentMax, processingTimeNanos));
    }

    private double getAverageProcessingTimeNanos() {
        long totalMessages = messagesProcessed.sum();
        return totalMessages > 0 
            ? (double) processingTimeNanos.sum() / totalMessages 
            : 0.0;
    }

    /**
     * 度量快照类
     */
    public record MetricsSnapshot(
        long messagesProcessed,
        long messagesDropped,
        long messagesFailed,
        long currentMailboxSize,
        double averageProcessingTimeNanos,
        long maxProcessingTimeNanos,
        long failuresCount,
        long restartCount,
        long deadLettersCount,
        Instant startTime,
        Instant lastProcessedTime,
        Instant lastFailureTime
    ) {
        /**
         * 获取运行时间
         */
        public Duration getUptime() {
            return Duration.between(startTime, Instant.now());
        }

        /**
         * 获取消息处理率（每秒）
         */
        public double getMessagesPerSecond() {
            double uptimeSeconds = getUptime().toSeconds();
            return uptimeSeconds > 0 
                ? messagesProcessed / uptimeSeconds 
                : 0.0;
        }

        /**
         * 获取错误率（每秒）
         */
        public double getFailuresPerSecond() {
            double uptimeSeconds = getUptime().toSeconds();
            return uptimeSeconds > 0 
                ? failuresCount / uptimeSeconds 
                : 0.0;
        }

        /**
         * 获取平均处理时间（毫秒）
         */
        public double getAverageProcessingTimeMs() {
            return averageProcessingTimeNanos / 1_000_000.0;
        }

        /**
         * 获取最大处理时间（毫秒）
         */
        public double getMaxProcessingTimeMs() {
            return maxProcessingTimeNanos / 1_000_000.0;
        }

        @Override
        public String toString() {
            return String.format("""
                Actor Metrics:
                - Messages: processed=%d, dropped=%d, failed=%d
                - Current mailbox size: %d
                - Processing time (ms): avg=%.2f, max=%.2f
                - Failures: total=%d, restarts=%d
                - Dead letters: %d
                - Performance: %.2f msg/sec, %.2f failures/sec
                - Uptime: %s
                - Last processed: %s
                - Last failure: %s
                """,
                messagesProcessed, messagesDropped, messagesFailed,
                currentMailboxSize,
                getAverageProcessingTimeMs(), getMaxProcessingTimeMs(),
                failuresCount, restartCount,
                deadLettersCount,
                getMessagesPerSecond(), getFailuresPerSecond(),
                getUptime(),
                lastProcessedTime != null ? lastProcessedTime : "never",
                lastFailureTime != null ? lastFailureTime : "never"
            );
        }
    }
} 