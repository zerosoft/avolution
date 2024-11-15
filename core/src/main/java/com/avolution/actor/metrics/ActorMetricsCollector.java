package com.avolution.actor.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

public class ActorMetricsCollector {
    private final String actorPath;
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong signalCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, Duration> processingTimes = new ConcurrentHashMap<>();
    private final AtomicLong lastProcessedTimestamp = new AtomicLong(0);

    public ActorMetricsCollector(String actorPath) {
        this.actorPath = actorPath;
    }

    public void incrementMessageCount() {
        messageCount.incrementAndGet();
        updateLastProcessedTime();
    }

    public void incrementSignalCount() {
        signalCount.incrementAndGet();
        updateLastProcessedTime();
    }

    public void incrementFailureCount() {
        failureCount.incrementAndGet();
    }

    public void recordProcessingTime(String messageId, Duration duration) {
        processingTimes.put(messageId, duration);
        // 保持最近100条记录
        if (processingTimes.size() > 100) {
            String oldestKey = processingTimes.keySet().iterator().next();
            processingTimes.remove(oldestKey);
        }
    }

    private void updateLastProcessedTime() {
        lastProcessedTimestamp.set(System.currentTimeMillis());
    }

    public ActorMetrics getMetrics() {
        return new ActorMetrics(
                actorPath,
                messageCount.get(),
                signalCount.get(),
                failureCount.get(),
                calculateAverageProcessingTime(),
                lastProcessedTimestamp.get()
        );
    }

    private Duration calculateAverageProcessingTime() {
        if (processingTimes.isEmpty()) {
            return Duration.ZERO;
        }
        long totalNanos = processingTimes.values().stream()
                .mapToLong(Duration::toNanos)
                .sum();
        return Duration.ofNanos(totalNanos / processingTimes.size());
    }
}