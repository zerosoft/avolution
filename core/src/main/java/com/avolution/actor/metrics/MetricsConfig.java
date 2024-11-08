package com.avolution.actor.metrics;

import java.time.Duration;

public class MetricsConfig {
    private final boolean enabled;
    private final int sampleRate;
    private final boolean logMetrics;
    private final Duration logInterval;

    private MetricsConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.sampleRate = builder.sampleRate;
        this.logMetrics = builder.logMetrics;
        this.logInterval = builder.logInterval;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = false;
        private int sampleRate = 100;
        private boolean logMetrics = false;
        private Duration logInterval = Duration.ofMinutes(1);

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder sampleRate(int rate) {
            this.sampleRate = rate;
            return this;
        }

        public Builder logMetrics(boolean log) {
            this.logMetrics = log;
            return this;
        }

        public Builder logInterval(Duration interval) {
            this.logInterval = interval;
            return this;
        }

        public MetricsConfig build() {
            return new MetricsConfig(this);
        }
    }
}