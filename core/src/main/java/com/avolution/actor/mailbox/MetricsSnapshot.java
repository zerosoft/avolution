package com.avolution.actor.mailbox;

/**
 * 邮箱性能指标快照类
 * 
 * 该类提供了某一时刻邮箱的所有性能指标的不可变视图，包括：
 * - 消息统计
 * - 处理时间统计
 * - 状态变更统计
 * - 错误统计
 * 
 * 主要指标说明：
 * - totalMessages: 总消息数
 * - systemMessages: 系统消息数
 * - normalMessages: 普通消息数
 * - rejectedMessages: 被拒绝的消息数
 * - overflowMessages: 由于容量限制被拒绝的消息数
 * - errors: 错误次数
 * - totalProcessingTime: 总处理时间(纳秒)
 * - stashedMessages: 暂存消息数
 * - suspendCount: 暂停次数
 * - resumeCount: 恢复次数
 * 
 * 使用示例：
 * <pre>
 * {@code
 * MetricsSnapshot snapshot = mailbox.getMetrics().getSnapshot();
 * 
 * // 获取平均处理时间
 * double avgProcessingTime = snapshot.getAverageProcessingTimeNanos();
 * 
 * // 获取消息统计
 * long totalMessages = snapshot.getTotalMessages();
 * long errorCount = snapshot.getErrors();
 * 
 * // 输出统计信息
 * logger.info("Mailbox stats: {}", snapshot);
 * }
 * </pre>
 * 
 * 注意：
 * 1. 快照是不可变的，反映创建时刻的状态
 * 2. 建议定期创建快照用于监控
 * 3. 可以通过toString()方法获取格式化的统计信息
 */
public class MetricsSnapshot {
    private final long totalMessages;
    private final long systemMessages;
    private final long normalMessages;
    private final long rejectedMessages;
    private final long overflowMessages;
    private final long errors;
    private final long totalProcessingTime;
    private final long successfulEnqueues;
    private final long systemMessagesProcessed;
    private final long normalMessagesProcessed;
    private final long stashedMessages;
    private final long unstashedMessages;
    private final long suspendCount;
    private final long resumeCount;
    private final long clearCount;
    private final long closeCount;

    public MetricsSnapshot(
            long totalMessages, long systemMessages, long normalMessages,
            long rejectedMessages, long overflowMessages, long errors,
            long totalProcessingTime, long successfulEnqueues,
            long systemMessagesProcessed, long normalMessagesProcessed,
            long stashedMessages, long unstashedMessages,
            long suspendCount, long resumeCount,
            long clearCount, long closeCount) {
        this.totalMessages = totalMessages;
        this.systemMessages = systemMessages;
        this.normalMessages = normalMessages;
        this.rejectedMessages = rejectedMessages;
        this.overflowMessages = overflowMessages;
        this.errors = errors;
        this.totalProcessingTime = totalProcessingTime;
        this.successfulEnqueues = successfulEnqueues;
        this.systemMessagesProcessed = systemMessagesProcessed;
        this.normalMessagesProcessed = normalMessagesProcessed;
        this.stashedMessages = stashedMessages;
        this.unstashedMessages = unstashedMessages;
        this.suspendCount = suspendCount;
        this.resumeCount = resumeCount;
        this.clearCount = clearCount;
        this.closeCount = closeCount;
    }

    // Getters
    public long getTotalMessages() { return totalMessages; }
    public long getSystemMessages() { return systemMessages; }
    public long getNormalMessages() { return normalMessages; }
    public long getRejectedMessages() { return rejectedMessages; }
    public long getOverflowMessages() { return overflowMessages; }
    public long getErrors() { return errors; }
    public long getTotalProcessingTime() { return totalProcessingTime; }
    public long getSuccessfulEnqueues() { return successfulEnqueues; }
    public long getSystemMessagesProcessed() { return systemMessagesProcessed; }
    public long getNormalMessagesProcessed() { return normalMessagesProcessed; }
    public long getStashedMessages() { return stashedMessages; }
    public long getUnstashedMessages() { return unstashedMessages; }
    public long getSuspendCount() { return suspendCount; }
    public long getResumeCount() { return resumeCount; }
    public long getClearCount() { return clearCount; }
    public long getCloseCount() { return closeCount; }

    public double getAverageProcessingTimeNanos() {
        long processed = systemMessagesProcessed + normalMessagesProcessed;
        return processed > 0 ? (double) totalProcessingTime / processed : 0.0;
    }

    @Override
    public String toString() {
        return String.format(
            "MetricsSnapshot[total=%d, system=%d, normal=%d, rejected=%d, " +
            "overflow=%d, errors=%d, avgProcessingTime=%.2f ns]",
            totalMessages, systemMessages, normalMessages, rejectedMessages,
            overflowMessages, errors, getAverageProcessingTimeNanos()
        );
    }
} 