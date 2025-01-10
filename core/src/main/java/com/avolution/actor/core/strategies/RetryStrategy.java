package com.avolution.actor.core.strategies;

import com.avolution.actor.message.Envelope;

import java.time.Duration;

public interface RetryStrategy {

    boolean shouldRetry(Envelope envelope);

    int getMaxRetries();

    Duration getRetryDelay(int retryCount);
}
