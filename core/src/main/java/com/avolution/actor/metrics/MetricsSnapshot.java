package com.avolution.actor.metrics;

import java.time.Duration;
import java.time.Instant;

/**
 * Actor系统的度量指标快照，提供不可变的度量数据视图
 */
public record MetricsSnapshot(
        // Actor基本信息
        String actorPath,

        // 消息处理统计
        long totalMessages,
        long failedMessages,
        long deadLetters,

        // 处理时间统计（纳秒）
        long averageProcessingTime,
        long lastProcessingTime,
        long maxProcessingTime,
        long minProcessingTime,

        // 邮箱统计
        long messagesEnqueued,
        long messagesProcessed,
        long systemMessagesProcessed,
        long messagesRejected,
        double mailboxProcessingTimeMs,
        double mailboxMaxProcessingTimeMs,
        double messagesPerSecond,
        double failureRate,

        // 调度器统计
        long activeThreads,
        long totalTasks,
        long completedTasks,
        long failedTasks,
        long rejectedTasks,
        double averageTaskProcessingTime,

        // 时间戳
        Instant createdAt,
        Duration uptime
) {
    // 空快照单例
    public static final MetricsSnapshot EMPTY = new MetricsSnapshot(
            "unknown",
            0L, 0L, 0L,
            0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L,
            0.0, 0.0, 0.0, 0.0,
            0L, 0L, 0L, 0L, 0L, 0.0,
            Instant.now(),
            Duration.ZERO
    );

    /**
     * 创建新的快照构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 快照构建器
     */
    public static class Builder {
        private String actorPath = "unknown";
        private long totalMessages = 0;
        private long failedMessages = 0;
        private long deadLetters = 0;
        private long averageProcessingTime = 0;
        private long lastProcessingTime = 0;
        private long maxProcessingTime = 0;
        private long minProcessingTime = 0;
        private long messagesEnqueued = 0;
        private long messagesProcessed = 0;
        private long systemMessagesProcessed = 0;
        private long messagesRejected = 0;
        private double mailboxProcessingTimeMs = 0.0;
        private double mailboxMaxProcessingTimeMs = 0.0;
        private double messagesPerSecond = 0.0;
        private double failureRate = 0.0;
        private long activeThreads = 0;
        private long totalTasks = 0;
        private long completedTasks = 0;
        private long failedTasks = 0;
        private long rejectedTasks = 0;
        private double averageTaskProcessingTime = 0.0;
        private Instant createdAt = Instant.now();
        private Duration uptime = Duration.ZERO;

        public Builder actorPath(String actorPath) {
            this.actorPath = actorPath;
            return this;
        }

        public Builder totalMessages(long totalMessages) {
            this.totalMessages = totalMessages;
            return this;
        }

        public Builder failedMessages(long failedMessages) {
            this.failedMessages = failedMessages;
            return this;
        }

        public Builder deadLetters(long deadLetters) {
            this.deadLetters = deadLetters;
            return this;
        }

        public Builder averageProcessingTime(long averageProcessingTime) {
            this.averageProcessingTime = averageProcessingTime;
            return this;
        }

        public Builder lastProcessingTime(long lastProcessingTime) {
            this.lastProcessingTime = lastProcessingTime;
            return this;
        }

        public Builder maxProcessingTime(long maxProcessingTime) {
            this.maxProcessingTime = maxProcessingTime;
            return this;
        }

        public Builder minProcessingTime(long minProcessingTime) {
            this.minProcessingTime = minProcessingTime;
            return this;
        }

        public Builder messagesEnqueued(long messagesEnqueued) {
            this.messagesEnqueued = messagesEnqueued;
            return this;
        }

        public Builder messagesProcessed(long messagesProcessed) {
            this.messagesProcessed = messagesProcessed;
            return this;
        }

        public Builder systemMessagesProcessed(long systemMessagesProcessed) {
            this.systemMessagesProcessed = systemMessagesProcessed;
            return this;
        }

        public Builder messagesRejected(long messagesRejected) {
            this.messagesRejected = messagesRejected;
            return this;
        }

        public Builder mailboxProcessingTimeMs(double mailboxProcessingTimeMs) {
            this.mailboxProcessingTimeMs = mailboxProcessingTimeMs;
            return this;
        }

        public Builder mailboxMaxProcessingTimeMs(double mailboxMaxProcessingTimeMs) {
            this.mailboxMaxProcessingTimeMs = mailboxMaxProcessingTimeMs;
            return this;
        }

        public Builder messagesPerSecond(double messagesPerSecond) {
            this.messagesPerSecond = messagesPerSecond;
            return this;
        }

        public Builder failureRate(double failureRate) {
            this.failureRate = failureRate;
            return this;
        }

        public Builder activeThreads(long activeThreads) {
            this.activeThreads = activeThreads;
            return this;
        }

        public Builder totalTasks(long totalTasks) {
            this.totalTasks = totalTasks;
            return this;
        }

        public Builder completedTasks(long completedTasks) {
            this.completedTasks = completedTasks;
            return this;
        }

        public Builder failedTasks(long failedTasks) {
            this.failedTasks = failedTasks;
            return this;
        }

        public Builder rejectedTasks(long rejectedTasks) {
            this.rejectedTasks = rejectedTasks;
            return this;
        }

        public Builder averageTaskProcessingTime(double averageTaskProcessingTime) {
            this.averageTaskProcessingTime = averageTaskProcessingTime;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder uptime(Duration uptime) {
            this.uptime = uptime;
            return this;
        }


        public MetricsSnapshot build() {
            return new MetricsSnapshot(
                    actorPath, totalMessages, failedMessages, deadLetters,
                    averageProcessingTime, lastProcessingTime, maxProcessingTime, minProcessingTime,
                    messagesEnqueued, messagesProcessed, systemMessagesProcessed, messagesRejected,
                    mailboxProcessingTimeMs, mailboxMaxProcessingTimeMs, messagesPerSecond, failureRate,
                    activeThreads, totalTasks, completedTasks, failedTasks, rejectedTasks,
                    averageTaskProcessingTime, createdAt, uptime
            );
        }
    }
}