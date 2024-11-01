package com.avolution.actor.config;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 调度器配置
 */
public class DispatcherConfig {
    // 线程池配置
    private final int maxConcurrency;              // 最大并发数
    private final Duration taskTimeout;            // 任务超时
    private final Duration shutdownTimeout;        // 关闭超时
    private final boolean fairDispatch;            // 是否公平调度

    // 任务执行配置
    private final int maxBatchSize;                // 最大批处理大小
    private final Duration maxBatchWait;           // 最大批处理等待时间
    private final boolean throughputThrottling;    // 是否启用吞吐量限制
    private final int maxThroughputPerSecond;      // 每秒最大吞吐量

    // 监控配置
    private final boolean metricsEnabled;          // 是否启用度量
    private final Duration metricsInterval;        // 度量收集间隔
    private final int metricsHistorySize;          // 度量历史大小

    // 错误处理配置
    private final int maxRetries;                  // 最大重试次数
    private final Duration retryBackoff;           // 重试退避时间
    private final boolean rejectWhenFull;          // 队列满时是否拒绝

    private DispatcherConfig(Builder builder) {
        this.maxConcurrency = builder.maxConcurrency;
        this.taskTimeout = builder.taskTimeout;
        this.shutdownTimeout = builder.shutdownTimeout;
        this.fairDispatch = builder.fairDispatch;
        this.maxBatchSize = builder.maxBatchSize;
        this.maxBatchWait = builder.maxBatchWait;
        this.throughputThrottling = builder.throughputThrottling;
        this.maxThroughputPerSecond = builder.maxThroughputPerSecond;
        this.metricsEnabled = builder.metricsEnabled;
        this.metricsInterval = builder.metricsInterval;
        this.metricsHistorySize = builder.metricsHistorySize;
        this.maxRetries = builder.maxRetries;
        this.retryBackoff = builder.retryBackoff;
        this.rejectWhenFull = builder.rejectWhenFull;
    }

    // Getters
    public int getMaxConcurrency() { return maxConcurrency; }
    public Duration getTaskTimeout() { return taskTimeout; }
    public Duration getShutdownTimeout() { return shutdownTimeout; }
    public boolean isFairDispatch() { return fairDispatch; }
    public int getMaxBatchSize() { return maxBatchSize; }
    public Duration getMaxBatchWait() { return maxBatchWait; }
    public boolean isThroughputThrottling() { return throughputThrottling; }
    public int getMaxThroughputPerSecond() { return maxThroughputPerSecond; }
    public boolean isMetricsEnabled() { return metricsEnabled; }
    public Duration getMetricsInterval() { return metricsInterval; }
    public int getMetricsHistorySize() { return metricsHistorySize; }
    public int getMaxRetries() { return maxRetries; }
    public Duration getRetryBackoff() { return retryBackoff; }
    public boolean isRejectWhenFull() { return rejectWhenFull; }

    // 工厂方法
    public static Builder builder() {
        return new Builder();
    }

    public static DispatcherConfig defaultConfig() {
        return builder().build();
    }

    /**
     * Builder类
     */
    public static class Builder {
        private int maxConcurrency = Runtime.getRuntime().availableProcessors() * 2;
        private Duration taskTimeout = Duration.ofSeconds(5);
        private Duration shutdownTimeout = Duration.ofSeconds(10);
        private boolean fairDispatch = false;
        private int maxBatchSize = 100;
        private Duration maxBatchWait = Duration.ofMillis(50);
        private boolean throughputThrottling = false;
        private int maxThroughputPerSecond = 1000;
        private boolean metricsEnabled = true;
        private Duration metricsInterval = Duration.ofSeconds(1);
        private int metricsHistorySize = 100;
        private int maxRetries = 3;
        private Duration retryBackoff = Duration.ofMillis(100);
        private boolean rejectWhenFull = true;

        public Builder maxConcurrency(int concurrency) {
            this.maxConcurrency = concurrency;
            return this;
        }

        public Builder taskTimeout(Duration timeout) {
            this.taskTimeout = timeout;
            return this;
        }

        public Builder shutdownTimeout(Duration timeout) {
            this.shutdownTimeout = timeout;
            return this;
        }

        public Builder fairDispatch(boolean fair) {
            this.fairDispatch = fair;
            return this;
        }

        public Builder maxBatchSize(int size) {
            this.maxBatchSize = size;
            return this;
        }

        public Builder maxBatchWait(Duration wait) {
            this.maxBatchWait = wait;
            return this;
        }

        public Builder throughputThrottling(boolean throttling) {
            this.throughputThrottling = throttling;
            return this;
        }

        public Builder maxThroughputPerSecond(int throughput) {
            this.maxThroughputPerSecond = throughput;
            return this;
        }

        public Builder metricsEnabled(boolean enabled) {
            this.metricsEnabled = enabled;
            return this;
        }

        public Builder metricsInterval(Duration interval) {
            this.metricsInterval = interval;
            return this;
        }

        public Builder metricsHistorySize(int size) {
            this.metricsHistorySize = size;
            return this;
        }

        public Builder maxRetries(int retries) {
            this.maxRetries = retries;
            return this;
        }

        public Builder retryBackoff(Duration backoff) {
            this.retryBackoff = backoff;
            return this;
        }

        public Builder rejectWhenFull(boolean reject) {
            this.rejectWhenFull = reject;
            return this;
        }

        public DispatcherConfig build() {
            validate();
            return new DispatcherConfig(this);
        }

        private void validate() {
            if (maxConcurrency <= 0) {
                throw new IllegalArgumentException("Max concurrency must be positive");
            }
            if (maxBatchSize <= 0) {
                throw new IllegalArgumentException("Max batch size must be positive");
            }
            if (maxThroughputPerSecond <= 0 && throughputThrottling) {
                throw new IllegalArgumentException("Max throughput must be positive when throttling is enabled");
            }
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries must be non-negative");
            }
            if (metricsHistorySize <= 0) {
                throw new IllegalArgumentException("Metrics history size must be positive");
            }
            Objects.requireNonNull(taskTimeout, "Task timeout cannot be null");
            Objects.requireNonNull(shutdownTimeout, "Shutdown timeout cannot be null");
            Objects.requireNonNull(maxBatchWait, "Max batch wait cannot be null");
            Objects.requireNonNull(metricsInterval, "Metrics interval cannot be null");
            Objects.requireNonNull(retryBackoff, "Retry backoff cannot be null");
        }
    }

    @Override
    public String toString() {
        return String.format("""
            DispatcherConfig {
                maxConcurrency: %d
                taskTimeout: %s
                shutdownTimeout: %s
                fairDispatch: %b
                maxBatchSize: %d
                maxBatchWait: %s
                throughputThrottling: %b
                maxThroughputPerSecond: %d
                metricsEnabled: %b
                metricsInterval: %s
                metricsHistorySize: %d
                maxRetries: %d
                retryBackoff: %s
                rejectWhenFull: %b
            }""",
                maxConcurrency, taskTimeout, shutdownTimeout, fairDispatch,
                maxBatchSize, maxBatchWait, throughputThrottling, maxThroughputPerSecond,
                metricsEnabled, metricsInterval, metricsHistorySize,
                maxRetries, retryBackoff, rejectWhenFull
        );
    }
} 