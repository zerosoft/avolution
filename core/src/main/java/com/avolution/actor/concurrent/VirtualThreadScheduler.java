package com.avolution.actor.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 虚拟线程调度器
 */
public class VirtualThreadScheduler implements ScheduledExecutorService {
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isShutdown;
    private final AtomicInteger activeTaskCount;

    public VirtualThreadScheduler(String nameFix){

        ThreadFactory factory = Thread.ofVirtual().name(nameFix,1L).factory();
        this.executor = Executors.newThreadPerTaskExecutor(factory);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("VirtualThreadScheduler")::unstarted);

        this.isShutdown = new AtomicBoolean(false);
        this.activeTaskCount = new AtomicInteger(0);
    }

    public VirtualThreadScheduler() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("VirtualThreadScheduler")::unstarted);

        this.isShutdown = new AtomicBoolean(false);
        this.activeTaskCount = new AtomicInteger(0);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        checkShutdown();
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (!isShutdown.get()) {
                executeTask(command);
            }
        }, delay, unit);
        return new DelegatingScheduledFuture<>(future);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        checkShutdown();
        ScheduledFuture<V> future = scheduler.schedule(() -> {
            if (!isShutdown.get()) {
                return executeTask(callable);
            }
            throw new CancellationException("Scheduler is shutdown");
        }, delay, unit);
        return new DelegatingScheduledFuture<>(future);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        checkShutdown();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (!isShutdown.get()) {
                executeTask(command);
            }
        }, initialDelay, period, unit);
        return new DelegatingScheduledFuture<>(future);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        checkShutdown();
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            if (!isShutdown.get()) {
                executeTask(command);
            }
        }, initialDelay, delay, unit);
        return new DelegatingScheduledFuture<>(future);
    }

    private void executeTask(Runnable task) {
        activeTaskCount.incrementAndGet();
        executor.execute(() -> {
            try {
                task.run();
            } finally {
                activeTaskCount.decrementAndGet();
            }
        });
    }

    private <V> V executeTask(Callable<V> task) throws Exception {
        activeTaskCount.incrementAndGet();
        try {
            Future<V> future = executor.submit(task);
            return future.get();
        } finally {
            activeTaskCount.decrementAndGet();
        }
    }

    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            scheduler.shutdown();
            executor.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown.set(true);
        List<Runnable> tasks = scheduler.shutdownNow();
        executor.shutdownNow();
        return tasks;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return isShutdown() && activeTaskCount.get() == 0 && 
               scheduler.isTerminated() && executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        
        if (!scheduler.awaitTermination(timeout, unit)) {
            return false;
        }
        
        long remaining = deadline - System.nanoTime();
        return executor.awaitTermination(remaining, TimeUnit.NANOSECONDS);
    }

    private void checkShutdown() {
        if (isShutdown()) {
            throw new RejectedExecutionException("Scheduler is shutdown");
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        checkShutdown();
        activeTaskCount.incrementAndGet();
        return executor.submit(() -> {
            try {
                return task.call();
            } finally {
                activeTaskCount.decrementAndGet();
            }
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        checkShutdown();
        activeTaskCount.incrementAndGet();
        return executor.submit(() -> {
            try {
                task.run();
                return result;
            } finally {
                activeTaskCount.decrementAndGet();
            }
        });
    }

    @Override
    public void execute(Runnable command) {
        checkShutdown();
        activeTaskCount.incrementAndGet();
        executor.execute(() -> {
            try {
                command.run();
            } finally {
                activeTaskCount.decrementAndGet();
            }
        });
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        checkShutdown();
        activeTaskCount.addAndGet(tasks.size());
        try {
            return executor.invokeAll(wrapTasks(tasks));
        } finally {
            activeTaskCount.addAndGet(-tasks.size());
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, 
                                        long timeout, TimeUnit unit)
            throws InterruptedException {
        checkShutdown();
        activeTaskCount.addAndGet(tasks.size());
        try {
            return executor.invokeAll(wrapTasks(tasks), timeout, unit);
        } finally {
            activeTaskCount.addAndGet(-tasks.size());
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        checkShutdown();
        activeTaskCount.incrementAndGet();
        try {
            return executor.invokeAny(wrapTasks(tasks));
        } finally {
            activeTaskCount.decrementAndGet();
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, 
                          long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        checkShutdown();
        activeTaskCount.incrementAndGet();
        try {
            return executor.invokeAny(wrapTasks(tasks), timeout, unit);
        } finally {
            activeTaskCount.decrementAndGet();
        }
    }

    private <T> Collection<Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        return tasks.stream()
            .map(task -> (Callable<T>) () -> {
                try {
                    return task.call();
                } finally {
                    activeTaskCount.decrementAndGet();
                }
            })
            .collect(Collectors.toList());
    }
} 