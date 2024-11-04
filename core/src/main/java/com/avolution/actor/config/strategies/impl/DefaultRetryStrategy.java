package com.avolution.actor.config.strategies.impl;

import com.avolution.actor.config.strategies.RetryStrategy;
import com.avolution.actor.message.Envelope;

import java.time.Duration;

public class DefaultRetryStrategy implements RetryStrategy {
    private final int maxRetries = 3;
    private final Duration baseDelay = Duration.ofMillis(100);

    @Override
    public boolean shouldRetry(Envelope envelope) {
        return envelope.retryCount() < maxRetries;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public Duration getRetryDelay(int retryCount) {
        return baseDelay.multipliedBy(1L << retryCount); // 指数退避
    }
}


