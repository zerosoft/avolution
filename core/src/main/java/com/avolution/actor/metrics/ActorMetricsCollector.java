package com.avolution.actor.metrics;


import com.avolution.actor.supervision.Directive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

public class ActorMetricsCollector {
    private final String actorPath;
    private final Counter messageCount;
    private final Counter signalCount;
    private final Counter failureCount;
    private final Counter deadLetterCount;
    private final Map<String, Counter> lifecycleEvents;
    private final Map<String, Timer> messageProcessingTimes;
    private final Map<Directive, Counter> supervisionDirectives;

    public ActorMetricsCollector(String actorPath) {
        this.actorPath = actorPath;
        this.messageCount = new Counter();
        this.signalCount = new Counter();
        this.failureCount = new Counter();
        this.deadLetterCount = new Counter();
        this.lifecycleEvents = new ConcurrentHashMap<>();
        this.messageProcessingTimes = new ConcurrentHashMap<>();
        this.supervisionDirectives = new ConcurrentHashMap<>();
    }

    public void incrementMessageCount() {
        messageCount.increment();
    }

    public void incrementSignalCount() {
        signalCount.increment();
    }

    public void recordMessageReceived(String actorPath) {
        messageProcessingTimes.computeIfAbsent(actorPath, k -> new Timer()).start();
    }

    public void recordMessageProcessed(String actorPath) {
        Timer timer = messageProcessingTimes.get(actorPath);
        if (timer != null) {
            timer.stop();
        }
    }

    public void recordMessageFailed(String actorPath) {
        failureCount.increment();
        Timer timer = messageProcessingTimes.get(actorPath);
        if (timer != null) {
            timer.stop();
        }
    }

    public void recordFailure(String actorPath, Throwable cause) {
        failureCount.increment();
        // 可以添加更详细的失败原因统计
    }

    public void recordLifecycleEvent(String actorPath, String event) {
        lifecycleEvents.computeIfAbsent(event, k -> new Counter()).increment();
    }

    public void recordResume(String actorPath) {
        supervisionDirectives.computeIfAbsent(Directive.RESUME, k -> new Counter()).increment();
    }

    public void recordRestart(String actorPath) {
        supervisionDirectives.computeIfAbsent(Directive.RESTART, k -> new Counter()).increment();
    }

    public void recordStop(String actorPath) {
        supervisionDirectives.computeIfAbsent(Directive.STOP, k -> new Counter()).increment();
    }

    public void recordEscalation(String actorPath) {
        supervisionDirectives.computeIfAbsent(Directive.ESCALATE, k -> new Counter()).increment();
    }

    // 内部计数器类
    private static class Counter {
        private final AtomicLong value = new AtomicLong();

        public void increment() {
            value.incrementAndGet();
        }

        public long getValue() {
            return value.get();
        }
    }

    // 内部计时器类
    private static class Timer {
        private final AtomicLong startTime = new AtomicLong();
        private final AtomicLong totalTime = new AtomicLong();
        private final AtomicInteger count = new AtomicInteger();

        public void start() {
            startTime.set(System.nanoTime());
        }

        public void stop() {
            long duration = System.nanoTime() - startTime.get();
            totalTime.addAndGet(duration);
            count.incrementAndGet();
        }

        public double getAverageTime() {
            int currentCount = count.get();
            return currentCount > 0 ? (double) totalTime.get() / currentCount : 0.0;
        }
    }
}