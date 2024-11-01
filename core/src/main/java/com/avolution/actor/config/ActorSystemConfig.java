package com.avolution.actor.config;

import java.time.Duration;
import java.util.Objects;

public class ActorSystemConfig {
    private final DispatcherConfig dispatcherConfig;
    private final int schedulerPoolSize;
    private final Duration shutdownTimeout;
    private final int defaultMailboxCapacity;
    private final boolean metricsEnabled;
    private final Duration metricsInterval;
    private final LoggingConfig loggingConfig;
    private final SerializationConfig serializationConfig;

    private ActorSystemConfig(Builder builder) {
        this.dispatcherConfig = builder.dispatcherConfig;
        this.schedulerPoolSize = builder.schedulerPoolSize;
        this.shutdownTimeout = builder.shutdownTimeout;
        this.defaultMailboxCapacity = builder.defaultMailboxCapacity;
        this.metricsEnabled = builder.metricsEnabled;
        this.metricsInterval = builder.metricsInterval;
        this.loggingConfig = builder.loggingConfig;
        this.serializationConfig = builder.serializationConfig;
    }

    // Getters
    public DispatcherConfig getDispatcherConfig() {
        return dispatcherConfig;
    }

    public int getSchedulerPoolSize() {
        return schedulerPoolSize;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public int getDefaultMailboxCapacity() {
        return defaultMailboxCapacity;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public Duration getMetricsInterval() {
        return metricsInterval;
    }

    public LoggingConfig getLoggingConfig() {
        return loggingConfig;
    }

    public SerializationConfig getSerializationConfig() {
        return serializationConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ActorSystemConfig defaultConfig() {
        return builder().build();
    }

    public static class Builder {
        private DispatcherConfig dispatcherConfig = DispatcherConfig.defaultConfig();
        private int schedulerPoolSize = Runtime.getRuntime().availableProcessors();
        private Duration shutdownTimeout = Duration.ofSeconds(10);
        private int defaultMailboxCapacity = 1000;
        private boolean metricsEnabled = true;
        private Duration metricsInterval = Duration.ofSeconds(1);
        private LoggingConfig loggingConfig = LoggingConfig.defaultConfig();
        private SerializationConfig serializationConfig = SerializationConfig.defaultConfig();

        public Builder dispatcherConfig(DispatcherConfig config) {
            this.dispatcherConfig = config;
            return this;
        }

        public Builder schedulerPoolSize(int size) {
            this.schedulerPoolSize = size;
            return this;
        }

        public Builder shutdownTimeout(Duration timeout) {
            this.shutdownTimeout = timeout;
            return this;
        }

        public Builder defaultMailboxCapacity(int capacity) {
            this.defaultMailboxCapacity = capacity;
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

        public Builder loggingConfig(LoggingConfig config) {
            this.loggingConfig = config;
            return this;
        }

        public Builder serializationConfig(SerializationConfig config) {
            this.serializationConfig = config;
            return this;
        }

        public ActorSystemConfig build() {
            validate();
            return new ActorSystemConfig(this);
        }

        private void validate() {
            Objects.requireNonNull(dispatcherConfig, "Dispatcher config cannot be null");
            Objects.requireNonNull(shutdownTimeout, "Shutdown timeout cannot be null");
            Objects.requireNonNull(metricsInterval, "Metrics interval cannot be null");
            Objects.requireNonNull(loggingConfig, "Logging config cannot be null");
            Objects.requireNonNull(serializationConfig, "Serialization config cannot be null");

            if (schedulerPoolSize <= 0) {
                throw new IllegalArgumentException("Scheduler pool size must be positive");
            }
            if (defaultMailboxCapacity <= 0) {
                throw new IllegalArgumentException("Default mailbox capacity must be positive");
            }
        }
    }
} 