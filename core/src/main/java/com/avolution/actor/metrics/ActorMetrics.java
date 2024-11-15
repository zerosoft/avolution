package com.avolution.actor.metrics;

import java.time.Duration;
import java.time.Instant;

public record ActorMetrics(
        String actorPath,
        long messageCount,
        long signalCount,
        long failureCount,
        Duration averageProcessingTime,
        long lastProcessedTimestamp
) {
    @Override
    public String toString() {
        return String.format(
                "ActorMetrics[path=%s, msgs=%d, signals=%d, failures=%d, avgTime=%dms, lastProcessed=%s]",
                actorPath,
                messageCount,
                signalCount,
                failureCount,
                averageProcessingTime.toMillis(),
                Instant.ofEpochMilli(lastProcessedTimestamp)
        );
    }
}
