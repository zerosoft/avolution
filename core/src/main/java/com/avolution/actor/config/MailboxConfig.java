package com.avolution.actor.config;

import com.avolution.actor.config.strategies.OverflowStrategy;
import com.avolution.actor.config.strategies.PriorityStrategy;
import com.avolution.actor.config.strategies.RetryStrategy;
import com.avolution.actor.config.strategies.StashStrategy;
import com.avolution.actor.config.strategies.impl.DefaultPriorityStrategy;
import com.avolution.actor.config.strategies.impl.DefaultRetryStrategy;
import com.avolution.actor.config.strategies.impl.DefaultStashStrategy;

import java.time.Duration;
import java.util.Objects;

/**
 * 邮箱配置类
 */
public class MailboxConfig {
    // 基本配置
    private final int capacity;                    // 邮箱容量
    private final int batchSize;                   // 批处理大小
    private final Duration messageTimeout;         // 消息处理超时
    private final Duration idleTimeout;            // 空闲超时

    // 策略配置
    private final OverflowStrategy overflowStrategy;       // 溢出策略
    private final RetryStrategy retryStrategy;             // 重试策略
    private final StashStrategy stashStrategy;             // 消息暂存策略
    private final PriorityStrategy priorityStrategy;       // 优先级策略

    // 监控配置
    private final boolean metricsEnabled;                  // 是否启用度量
    private final Duration metricsCollectionInterval;      // 度量收集间隔

    // 高级配置
    private final int systemMessageQueueCapacity;          // 系统消息队列容量
    private final Duration shutdownTimeout;                // 关闭超时
    private final boolean throughputThrottlingEnabled;     // 是否启用吞吐量限制
    private final int maxThroughputPerSecond;             // 每秒最大吞吐量

    private MailboxConfig(Builder builder) {
        this.capacity = builder.capacity;
        this.batchSize = builder.batchSize;
        this.messageTimeout = builder.messageTimeout;
        this.idleTimeout = builder.idleTimeout;
        this.overflowStrategy = builder.overflowStrategy;
        this.retryStrategy = builder.retryStrategy;
        this.stashStrategy = builder.stashStrategy;
        this.priorityStrategy = builder.priorityStrategy;
        this.metricsEnabled = builder.metricsEnabled;
        this.metricsCollectionInterval = builder.metricsCollectionInterval;
        this.systemMessageQueueCapacity = builder.systemMessageQueueCapacity;
        this.shutdownTimeout = builder.shutdownTimeout;
        this.throughputThrottlingEnabled = builder.throughputThrottlingEnabled;
        this.maxThroughputPerSecond = builder.maxThroughputPerSecond;
    }

    // Getters
    public int getCapacity() {
        return capacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Duration getMessageTimeout() {
        return messageTimeout;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public OverflowStrategy getOverflowStrategy() {
        return overflowStrategy;
    }

    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    public StashStrategy getStashStrategy() {
        return stashStrategy;
    }

    public PriorityStrategy getPriorityStrategy() {
        return priorityStrategy;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public Duration getMetricsCollectionInterval() {
        return metricsCollectionInterval;
    }

    public int getSystemMessageQueueCapacity() {
        return systemMessageQueueCapacity;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public boolean isThroughputThrottlingEnabled() {
        return throughputThrottlingEnabled;
    }

    public int getMaxThroughputPerSecond() {
        return maxThroughputPerSecond;
    }

    // 工厂方法
    public static Builder builder() {
        return new Builder();
    }

    public static MailboxConfig defaultConfig() {
        return builder().build();
    }

    /**
     * Builder类
     */
    public static class Builder {
        // 默认值设置
        private int capacity = 1000;
        private int batchSize = 10;
        private Duration messageTimeout = Duration.ofSeconds(5);
        private Duration idleTimeout = Duration.ofMinutes(1);
        private OverflowStrategy overflowStrategy = OverflowStrategy.DROP_NEW;
        private RetryStrategy retryStrategy = new DefaultRetryStrategy();
        private StashStrategy stashStrategy = new DefaultStashStrategy();
        private PriorityStrategy priorityStrategy = new DefaultPriorityStrategy();
        private boolean metricsEnabled = true;
        private Duration metricsCollectionInterval = Duration.ofSeconds(1);
        private int systemMessageQueueCapacity = 100;
        private Duration shutdownTimeout = Duration.ofSeconds(10);
        private boolean throughputThrottlingEnabled = false;
        private int maxThroughputPerSecond = 1000;

        // Builder methods
        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder messageTimeout(Duration timeout) {
            this.messageTimeout = timeout;
            return this;
        }

        public Builder idleTimeout(Duration timeout) {
            this.idleTimeout = timeout;
            return this;
        }

        public Builder overflowStrategy(OverflowStrategy strategy) {
            this.overflowStrategy = strategy;
            return this;
        }

        public Builder retryStrategy(RetryStrategy strategy) {
            this.retryStrategy = strategy;
            return this;
        }

        public Builder stashStrategy(StashStrategy strategy) {
            this.stashStrategy = strategy;
            return this;
        }

        public Builder priorityStrategy(PriorityStrategy strategy) {
            this.priorityStrategy = strategy;
            return this;
        }

        public Builder metricsEnabled(boolean enabled) {
            this.metricsEnabled = enabled;
            return this;
        }

        public Builder metricsCollectionInterval(Duration interval) {
            this.metricsCollectionInterval = interval;
            return this;
        }

        public Builder systemMessageQueueCapacity(int capacity) {
            this.systemMessageQueueCapacity = capacity;
            return this;
        }

        public Builder shutdownTimeout(Duration timeout) {
            this.shutdownTimeout = timeout;
            return this;
        }

        public Builder throughputThrottlingEnabled(boolean enabled) {
            this.throughputThrottlingEnabled = enabled;
            return this;
        }

        public Builder maxThroughputPerSecond(int maxThroughput) {
            this.maxThroughputPerSecond = maxThroughput;
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
            if (batchSize <= 0 || batchSize > capacity) {
                throw new IllegalArgumentException("Invalid batch size");
            }
            if (systemMessageQueueCapacity <= 0) {
                throw new IllegalArgumentException("System message queue capacity must be positive");
            }
            Objects.requireNonNull(messageTimeout, "Message timeout cannot be null");
            Objects.requireNonNull(idleTimeout, "Idle timeout cannot be null");
            Objects.requireNonNull(overflowStrategy, "Overflow strategy cannot be null");
            Objects.requireNonNull(retryStrategy, "Retry strategy cannot be null");
            Objects.requireNonNull(stashStrategy, "Stash strategy cannot be null");
            Objects.requireNonNull(priorityStrategy, "Priority strategy cannot be null");
            Objects.requireNonNull(metricsCollectionInterval, "Metrics collection interval cannot be null");
            Objects.requireNonNull(shutdownTimeout, "Shutdown timeout cannot be null");

            if (maxThroughputPerSecond <= 0 && throughputThrottlingEnabled) {
                throw new IllegalArgumentException("Max throughput must be positive when throttling is enabled");
            }
        }
    }

    @Override
    public String toString() {
        return String.format("""
                        MailboxConfig {
                            capacity: %d
                            batchSize: %d
                            messageTimeout: %s
                            idleTimeout: %s
                            overflowStrategy: %s
                            retryStrategy: %s
                            stashStrategy: %s
                            priorityStrategy: %s
                            metricsEnabled: %b
                            metricsCollectionInterval: %s
                            systemMessageQueueCapacity: %d
                            shutdownTimeout: %s
                            throughputThrottlingEnabled: %b
                            maxThroughputPerSecond: %d
                        }""",
                capacity, batchSize, messageTimeout, idleTimeout,
                overflowStrategy, retryStrategy, stashStrategy, priorityStrategy,
                metricsEnabled, metricsCollectionInterval,
                systemMessageQueueCapacity, shutdownTimeout,
                throughputThrottlingEnabled, maxThroughputPerSecond
        );
    }
}