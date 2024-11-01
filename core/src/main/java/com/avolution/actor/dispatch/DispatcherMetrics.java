package com.avolution.actor.dispatch;

import java.util.concurrent.atomic.LongAdder;

public class DispatcherMetrics {
    private final LongAdder activeThreads = new LongAdder();
    private final LongAdder totalTasks = new LongAdder();
    private final LongAdder completedTasks = new LongAdder();
    private final LongAdder failedTasks = new LongAdder();
    private final LongAdder rejectedTasks = new LongAdder();
    private final LongAdder totalProcessingTime = new LongAdder();

    void taskStarted() {
        activeThreads.increment();
        totalTasks.increment();
    }

    void taskCompleted(long processingTimeNanos) {
        activeThreads.decrement();
        completedTasks.increment();
        totalProcessingTime.add(processingTimeNanos);
    }

    void taskFailed() {
        activeThreads.decrement();
        failedTasks.increment();
    }

    void taskRejected() {
        rejectedTasks.increment();
    }

    public long getActiveThreads() {
        return activeThreads.sum();
    }

    public long getTotalTasks() {
        return totalTasks.sum();
    }

    public long getCompletedTasks() {
        return completedTasks.sum();
    }

    public long getFailedTasks() {
        return failedTasks.sum();
    }

    public long getRejectedTasks() {
        return rejectedTasks.sum();
    }

    public double getAverageProcessingTimeNanos() {
        long completed = completedTasks.sum();
        return completed > 0 ?
                (double) totalProcessingTime.sum() / completed : 0.0;
    }
} 