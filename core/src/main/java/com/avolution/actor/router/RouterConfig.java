package com.avolution.actor.router;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 路由器配置基类
 */
public abstract class RouterConfig {
    private final int poolSize;                    // 路由池大小
    private final ResizeStrategy resizeStrategy;   // 调整大小策略
    private final Duration resizeInterval;         // 调整间隔
    private final boolean dynamicRouting;          // 是否动态路由
    private final int minPoolSize;                 // 最小池大小
    private final int maxPoolSize;                 // 最大池大小
    private final boolean metricsEnabled;          // 是否启用度量
    private final Duration routingTimeout;         // 路由超时
    private final boolean useVirtualNodes;         // 是否使用虚拟节点(一致性哈希)

    protected RouterConfig(Builder<?> builder) {
        this.poolSize = builder.poolSize;
        this.resizeStrategy = builder.resizeStrategy;
        this.resizeInterval = builder.resizeInterval;
        this.dynamicRouting = builder.dynamicRouting;
        this.minPoolSize = builder.minPoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.metricsEnabled = builder.metricsEnabled;
        this.routingTimeout = builder.routingTimeout;
        this.useVirtualNodes = builder.useVirtualNodes;
    }

    // 抽象方法，由具体路由策略实现
    public abstract ActorRef selectRoutee(List<ActorRef> routees, Envelope message);
    public abstract String getRoutingLogic();

    // Getters
    public int getPoolSize() { return poolSize; }
    public ResizeStrategy getResizeStrategy() { return resizeStrategy; }
    public Duration getResizeInterval() { return resizeInterval; }
    public boolean isDynamicRouting() { return dynamicRouting; }
    public int getMinPoolSize() { return minPoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public boolean isMetricsEnabled() { return metricsEnabled; }
    public Duration getRoutingTimeout() { return routingTimeout; }
    public boolean isUseVirtualNodes() { return useVirtualNodes; }

    /**
     * 抽象Builder类
     */
    public abstract static class Builder<T extends Builder<T>> {
        private int poolSize = 4;
        private ResizeStrategy resizeStrategy = ResizeStrategy.NONE;
        private Duration resizeInterval = Duration.ofMinutes(1);
        private boolean dynamicRouting = false;
        private int minPoolSize = 1;
        private int maxPoolSize = 10;
        private boolean metricsEnabled = true;
        private Duration routingTimeout = Duration.ofSeconds(5);
        private boolean useVirtualNodes = false;

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T poolSize(int size) {
            this.poolSize = size;
            return self();
        }

        public T resizeStrategy(ResizeStrategy strategy) {
            this.resizeStrategy = strategy;
            return self();
        }

        public T resizeInterval(Duration interval) {
            this.resizeInterval = interval;
            return self();
        }

        public T dynamicRouting(boolean dynamic) {
            this.dynamicRouting = dynamic;
            return self();
        }

        public T minPoolSize(int size) {
            this.minPoolSize = size;
            return self();
        }

        public T maxPoolSize(int size) {
            this.maxPoolSize = size;
            return self();
        }

        public T metricsEnabled(boolean enabled) {
            this.metricsEnabled = enabled;
            return self();
        }

        public T routingTimeout(Duration timeout) {
            this.routingTimeout = timeout;
            return self();
        }

        public T useVirtualNodes(boolean use) {
            this.useVirtualNodes = use;
            return self();
        }

        protected void validate() {
            if (poolSize <= 0) {
                throw new IllegalArgumentException("Pool size must be positive");
            }
            if (minPoolSize <= 0) {
                throw new IllegalArgumentException("Min pool size must be positive");
            }
            if (maxPoolSize < minPoolSize) {
                throw new IllegalArgumentException("Max pool size must be >= min pool size");
            }
            if (poolSize < minPoolSize || poolSize > maxPoolSize) {
                throw new IllegalArgumentException("Pool size must be between min and max pool size");
            }
            Objects.requireNonNull(resizeStrategy, "Resize strategy cannot be null");
            Objects.requireNonNull(resizeInterval, "Resize interval cannot be null");
            Objects.requireNonNull(routingTimeout, "Routing timeout cannot be null");
        }

        public abstract RouterConfig build();
    }
}

