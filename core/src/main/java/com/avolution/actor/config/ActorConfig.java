package com.avolution.actor.config;

import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.router.RouterConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Actor配置类，定义Actor的行为和特性
 */
public class ActorConfig {
    // 基础配置
    private final String name;                         // Actor名称
    private final Class<?> actorClass;                // Actor类
    private final MailboxConfig mailboxConfig;        // 邮箱配置
    private final DispatcherConfig dispatcherConfig;  // 调度器配置

    // 生命周期配置
    private final Duration startTimeout;              // 启动超时
    private final Duration stopTimeout;               // 停止超时
    private final Duration restartDelay;              // 重启延迟
    private final int maxRestarts;                    // 最大重启次数
    private final Duration restartWindow;             // 重启窗口时间

    // 监督配置
    private final SupervisorStrategy supervisorStrategy;  // 监督策略
    private final boolean supervisionEnabled;            // 是否启用监督

    // 路由配置
    private final Optional<RouterConfig> routerConfig;   // 路由配置
    private final int routingPoolSize;                   // 路由池大小

    // 高级特性
    private final boolean persistenceEnabled;            // 是否启用持久化
    private final boolean stashEnabled;                  // 是否启用消息暂存
    private final boolean metricsEnabled;                // 是否启用度量收集
    private final Duration metricsInterval;              // 度量收集间隔
    private final int stashCapacity;                     // 暂存容量
    private final boolean throughputThrottlingEnabled;   // 是否启用吞吐量限制
    private final int maxThroughputPerSecond;           // 每秒最大吞吐量

    private ActorConfig(Builder builder) {
        this.name = builder.name;
        this.actorClass = builder.actorClass;
        this.mailboxConfig = builder.mailboxConfig;
        this.dispatcherConfig = builder.dispatcherConfig;
        this.startTimeout = builder.startTimeout;
        this.stopTimeout = builder.stopTimeout;
        this.restartDelay = builder.restartDelay;
        this.maxRestarts = builder.maxRestarts;
        this.restartWindow = builder.restartWindow;
        this.supervisorStrategy = builder.supervisorStrategy;
        this.supervisionEnabled = builder.supervisionEnabled;
        this.routerConfig = Optional.ofNullable(builder.routerConfig);
        this.routingPoolSize = builder.routingPoolSize;
        this.persistenceEnabled = builder.persistenceEnabled;
        this.stashEnabled = builder.stashEnabled;
        this.metricsEnabled = builder.metricsEnabled;
        this.metricsInterval = builder.metricsInterval;
        this.stashCapacity = builder.stashCapacity;
        this.throughputThrottlingEnabled = builder.throughputThrottlingEnabled;
        this.maxThroughputPerSecond = builder.maxThroughputPerSecond;
    }

    // Getters
    public String getName() {
        return name;
    }

    public Class<?> getActorClass() {
        return actorClass;
    }

    public MailboxConfig getMailboxConfig() {
        return mailboxConfig;
    }

    public DispatcherConfig getDispatcherConfig() {
        return dispatcherConfig;
    }

    public Duration getStartTimeout() {
        return startTimeout;
    }

    public Duration getStopTimeout() {
        return stopTimeout;
    }

    public Duration getRestartDelay() {
        return restartDelay;
    }

    public int getMaxRestarts() {
        return maxRestarts;
    }

    public Duration getRestartWindow() {
        return restartWindow;
    }

    public SupervisorStrategy getSupervisorStrategy() {
        return supervisorStrategy;
    }

    public boolean isSupervisionEnabled() {
        return supervisionEnabled;
    }

    public Optional<RouterConfig> getRouterConfig() {
        return routerConfig;
    }

    public int getRoutingPoolSize() {
        return routingPoolSize;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public boolean isStashEnabled() {
        return stashEnabled;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public Duration getMetricsInterval() {
        return metricsInterval;
    }

    public int getStashCapacity() {
        return stashCapacity;
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

    public static ActorConfig defaultConfig(String name, Class<?> actorClass) {
        return builder()
                .name(name)
                .actorClass(actorClass)
                .build();
    }

    /**
     * Builder类
     */
    public static class Builder {
        // 必需参数
        private String name;
        private Class<?> actorClass;

        // 可选参数及其默认值
        private MailboxConfig mailboxConfig = MailboxConfig.defaultConfig();
        private DispatcherConfig dispatcherConfig = DispatcherConfig.defaultConfig();
        private Duration startTimeout = Duration.ofSeconds(5);
        private Duration stopTimeout = Duration.ofSeconds(5);
        private Duration restartDelay = Duration.ofMillis(100);
        private int maxRestarts = 10;
        private Duration restartWindow = Duration.ofMinutes(1);
        private SupervisorStrategy supervisorStrategy = new DefaultSupervisorStrategy();
        private boolean supervisionEnabled = true;
        private RouterConfig routerConfig = null;
        private int routingPoolSize = 4;
        private boolean persistenceEnabled = false;
        private boolean stashEnabled = false;
        private boolean metricsEnabled = true;
        private Duration metricsInterval = Duration.ofSeconds(1);
        private int stashCapacity = 100;
        private boolean throughputThrottlingEnabled = false;
        private int maxThroughputPerSecond = 1000;

        // Builder methods
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder actorClass(Class<?> actorClass) {
            this.actorClass = actorClass;
            return this;
        }

        public Builder mailboxConfig(MailboxConfig config) {
            this.mailboxConfig = config;
            return this;
        }

        public Builder dispatcherConfig(DispatcherConfig config) {
            this.dispatcherConfig = config;
            return this;
        }

        public Builder startTimeout(Duration timeout) {
            this.startTimeout = timeout;
            return this;
        }

        public Builder stopTimeout(Duration timeout) {
            this.stopTimeout = timeout;
            return this;
        }

        public Builder restartDelay(Duration delay) {
            this.restartDelay = delay;
            return this;
        }

        public Builder maxRestarts(int max) {
            this.maxRestarts = max;
            return this;
        }

        public Builder restartWindow(Duration window) {
            this.restartWindow = window;
            return this;
        }

        public Builder supervisorStrategy(SupervisorStrategy strategy) {
            this.supervisorStrategy = strategy;
            return this;
        }

        public Builder supervisionEnabled(boolean enabled) {
            this.supervisionEnabled = enabled;
            return this;
        }

        public Builder routerConfig(RouterConfig config) {
            this.routerConfig = config;
            return this;
        }

        public Builder routingPoolSize(int size) {
            this.routingPoolSize = size;
            return this;
        }

        public Builder persistenceEnabled(boolean enabled) {
            this.persistenceEnabled = enabled;
            return this;
        }

        public Builder stashEnabled(boolean enabled) {
            this.stashEnabled = enabled;
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

        public Builder stashCapacity(int capacity) {
            this.stashCapacity = capacity;
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

        public ActorConfig build() {
            validate();
            return new ActorConfig(this);
        }

        private void validate() {
            Objects.requireNonNull(name, "Actor name cannot be null");
            Objects.requireNonNull(actorClass, "Actor class cannot be null");
            Objects.requireNonNull(mailboxConfig, "Mailbox config cannot be null");
            Objects.requireNonNull(dispatcherConfig, "Dispatcher config cannot be null");
            Objects.requireNonNull(startTimeout, "Start timeout cannot be null");
            Objects.requireNonNull(stopTimeout, "Stop timeout cannot be null");
            Objects.requireNonNull(restartDelay, "Restart delay cannot be null");
            Objects.requireNonNull(restartWindow, "Restart window cannot be null");
            Objects.requireNonNull(supervisorStrategy, "Supervisor strategy cannot be null");
            Objects.requireNonNull(metricsInterval, "Metrics interval cannot be null");

            if (maxRestarts < 0) {
                throw new IllegalArgumentException("Max restarts must be non-negative");
            }
            if (routingPoolSize <= 0) {
                throw new IllegalArgumentException("Routing pool size must be positive");
            }
            if (stashCapacity <= 0) {
                throw new IllegalArgumentException("Stash capacity must be positive");
            }
            if (maxThroughputPerSecond <= 0 && throughputThrottlingEnabled) {
                throw new IllegalArgumentException("Max throughput must be positive when throttling is enabled");
            }
        }
    }

    @Override
    public String toString() {
        return String.format("""
                        ActorConfig {
                            name: %s
                            actorClass: %s
                            mailboxConfig: %s
                            dispatcherConfig: %s
                            startTimeout: %s
                            stopTimeout: %s
                            restartDelay: %s
                            maxRestarts: %d
                            restartWindow: %s
                            supervisionEnabled: %b
                            routingPoolSize: %d
                            persistenceEnabled: %b
                            stashEnabled: %b
                            metricsEnabled: %b
                            metricsInterval: %s
                            stashCapacity: %d
                            throughputThrottlingEnabled: %b
                            maxThroughputPerSecond: %d
                        }""",
                name, actorClass.getSimpleName(), mailboxConfig, dispatcherConfig,
                startTimeout, stopTimeout, restartDelay, maxRestarts, restartWindow,
                supervisionEnabled, routingPoolSize, persistenceEnabled,
                stashEnabled, metricsEnabled, metricsInterval, stashCapacity,
                throughputThrottlingEnabled, maxThroughputPerSecond
        );
    }
}