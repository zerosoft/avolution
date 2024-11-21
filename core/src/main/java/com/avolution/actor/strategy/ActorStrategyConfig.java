package com.avolution.actor.strategy;

import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;

import java.time.Duration;

public class ActorStrategyConfig {
    private SupervisorStrategy supervisorStrategy;
    private boolean metricsEnabled = true;
    private Duration metricsInterval = Duration.ofSeconds(10);
    private int maxRetries = 3;
    private Duration retryWindow = Duration.ofMinutes(1);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ActorStrategyConfig config = new ActorStrategyConfig();

        public Builder withSupervisorStrategy(SupervisorStrategy strategy) {
            config.supervisorStrategy = strategy;
            return this;
        }

        public Builder withMetricsEnabled(boolean enabled) {
            config.metricsEnabled = enabled;
            return this;
        }

        public Builder withMetricsInterval(Duration interval) {
            config.metricsInterval = interval;
            return this;
        }

        public Builder withMaxRetries(int maxRetries) {
            config.maxRetries = maxRetries;
            return this;
        }

        public Builder withRetryWindow(Duration window) {
            config.retryWindow = window;
            return this;
        }

        public ActorStrategyConfig build() {
            if (config.supervisorStrategy == null) {
                config.supervisorStrategy = new DefaultSupervisorStrategy();
            }
            return config;
        }
    }
}