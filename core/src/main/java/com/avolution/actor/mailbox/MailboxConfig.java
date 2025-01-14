package com.avolution.actor.mailbox;

/**
 * 邮箱配置类
 * 
 * 该类使用Builder模式提供了邮箱的所有可配置参数，包括：
 * - 容量限制
 * - 吞吐量控制
 * - 重试策略
 * - 消息超时设置
 * 
 * 配置参数说明：
 * - capacity: 邮箱最大容量
 * - throughputEnabled: 是否启用吞吐量控制
 * - throughputLimit: 每批处理的最大消息数
 * - retryAttempts: 消息处理失败时的重试次数
 * - retryDelayMs: 重试间隔时间(毫秒)
 * - initialQueueSize: 初始队列大小
 * - messageTimeout: 消息处理超时时间(毫秒)
 * 
 * 使用示例：
 * <pre>
 * {@code
 * MailboxConfig config = MailboxConfig.builder()
 *     .capacity(1000)
 *     .throughputEnabled(true)
 *     .throughputLimit(100)
 *     .retryAttempts(3)
 *     .retryDelayMs(100)
 *     .messageTimeout(30000)
 *     .build();
 * 
 * Mailbox mailbox = new Mailbox(config);
 * }
 * </pre>
 * 
 * 注意：
 * 1. 所有参数都有合理的默认值
 * 2. build()时会进行参数验证
 * 3. 配置一旦创建就不可修改
 */
public class MailboxConfig {
    private final int capacity;
    private final boolean throughputEnabled;
    private final int throughputLimit;
    private final int retryAttempts;
    private final long retryDelayMs;
    private final int initialQueueSize;
    private final long messageTimeout;

    private MailboxConfig(Builder builder) {
        this.capacity = builder.capacity;
        this.throughputEnabled = builder.throughputEnabled;
        this.throughputLimit = builder.throughputLimit;
        this.retryAttempts = builder.retryAttempts;
        this.retryDelayMs = builder.retryDelayMs;
        this.initialQueueSize = builder.initialQueueSize;
        this.messageTimeout = builder.messageTimeout;
    }

    // Getters
    public int getCapacity() { return capacity; }
    public boolean isThroughputEnabled() { return throughputEnabled; }
    public int getThroughputLimit() { return throughputLimit; }
    public int getRetryAttempts() { return retryAttempts; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public int getInitialQueueSize() { return initialQueueSize; }
    public long getMessageTimeout() { return messageTimeout; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int capacity = 1000;
        private boolean throughputEnabled = true;
        private int throughputLimit = 100;
        private int retryAttempts = 3;
        private long retryDelayMs = 100;
        private int initialQueueSize = 16;
        private long messageTimeout = 30000; // 30 seconds

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder throughputEnabled(boolean enabled) {
            this.throughputEnabled = enabled;
            return this;
        }

        public Builder throughputLimit(int limit) {
            this.throughputLimit = limit;
            return this;
        }

        public Builder retryAttempts(int attempts) {
            this.retryAttempts = attempts;
            return this;
        }

        public Builder retryDelayMs(long delay) {
            this.retryDelayMs = delay;
            return this;
        }

        public Builder initialQueueSize(int size) {
            this.initialQueueSize = size;
            return this;
        }

        public Builder messageTimeout(long timeout) {
            this.messageTimeout = timeout;
            return this;
        }

        public MailboxConfig build() {
            validate();
            return new MailboxConfig(this);
        }

        private void validate() {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            if (throughputLimit <= 0) {
                throw new IllegalArgumentException("Throughput limit must be positive");
            }
            if (retryAttempts < 0) {
                throw new IllegalArgumentException("Retry attempts cannot be negative");
            }
            if (retryDelayMs < 0) {
                throw new IllegalArgumentException("Retry delay cannot be negative");
            }
            if (initialQueueSize <= 0) {
                throw new IllegalArgumentException("Initial queue size must be positive");
            }
            if (messageTimeout <= 0) {
                throw new IllegalArgumentException("Message timeout must be positive");
            }
        }
    }
} 