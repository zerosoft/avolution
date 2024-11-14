package com.avolution.actor.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.*;

public class ActorMetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(ActorMetricsCollector.class);

    private final String actorPath;
    private final AtomicBoolean enabled;
    private final AtomicLong messageCount;
    private final AtomicLong failureCount;
    private final AtomicLong totalProcessingTime;
    private final AtomicLong maxProcessingTime;
    private final AtomicLong minProcessingTime;
    private volatile long lastProcessingTime;
    private final AtomicInteger deadLetterCount;

    public ActorMetricsCollector(String actorPath) {
        this.actorPath = actorPath;
        this.enabled = new AtomicBoolean(true);
        this.messageCount = new AtomicLong(0);
        this.failureCount = new AtomicLong(0);
        this.totalProcessingTime = new AtomicLong(0);
        this.maxProcessingTime = new AtomicLong(0);
        this.minProcessingTime = new AtomicLong(Long.MAX_VALUE);
        this.deadLetterCount = new AtomicInteger(0);
        this.lastProcessingTime = 0;
    }

    public void recordMessage(long startTime, boolean success) {
        if (!enabled.get()) return;

        long processingTime = System.nanoTime() - startTime;
        messageCount.incrementAndGet();
        if (!success) {
            failureCount.incrementAndGet();
        }

        totalProcessingTime.addAndGet(processingTime);
        lastProcessingTime = processingTime;
        updateMaxProcessingTime(processingTime);
        updateMinProcessingTime(processingTime);
    }

    public void recordDeadLetter() {
        if (enabled.get()) {
            deadLetterCount.incrementAndGet();
        }
    }

    private void updateMaxProcessingTime(long time) {
        long  current;
        do {
            current = maxProcessingTime.get();
            if (time <= current) break;
        } while (!maxProcessingTime.compareAndSet(current, time));
    }

    private void updateMinProcessingTime(long time) {
        long current;
        do {
            current = minProcessingTime.get();
            if (time >= current) break;
        } while (!minProcessingTime.compareAndSet(current, time));
    }

    public MetricsSnapshot getSnapshot() {
        if (!enabled.get()){
            return MetricsSnapshot.EMPTY;
        }
        long total = messageCount.get();
        MetricsSnapshot.Builder builder = MetricsSnapshot.builder();
        builder.actorPath(actorPath)
                .totalMessages(total)
                .failedMessages(failureCount.get())
                .deadLetters(deadLetterCount.get())
                .averageProcessingTime(total == 0 ? 0 : totalProcessingTime.get() / total)
                .lastProcessingTime(lastProcessingTime)
                .maxProcessingTime(maxProcessingTime.get())
                .minProcessingTime(minProcessingTime.get());
        return builder.build();
    }

    public void reset() {
        messageCount.set(0);
        failureCount.set(0);
        totalProcessingTime.set(0);
        maxProcessingTime.set(0);
        minProcessingTime.set(Long.MAX_VALUE);
        deadLetterCount.set(0);
        lastProcessingTime = 0;
    }
}