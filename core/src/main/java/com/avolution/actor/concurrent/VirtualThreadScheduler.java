package com.avolution.actor.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 基于Java 21 虚拟线程的调度器实现
 * 提供异步任务调度和执行功能，支持定时任务和周期性任务
 * 
 * 主要特性：
 * 1. 使用虚拟线程处理任务，提供更好的并发性能
 * 2. 支持定时和周期性任务调度
 * 3. 提供任务计数和状态管理
 * 4. 优雅关闭机制
 */
public class VirtualThreadScheduler implements ScheduledExecutorService {
    
    /**
     * 用于执行实际任务的虚拟线程执行器
     */
    private final ExecutorService executor;
    
    /**
     * 用于调度定时任务的调度器
     */
    private final ScheduledExecutorService scheduler;
    
    /**
     * 调度器关闭状态标志
     */
    private final AtomicBoolean isShutdown;
    
    /**
     * 当前活动任务计数
     */
    private final AtomicInteger activeTaskCount;

    /**
     * 使用指定名称前缀创建调度器
     * @param namePrefix 线程名称前缀
     */
    public VirtualThreadScheduler(String namePrefix) {
        // 创建带有命名模式的虚拟线程工厂
        ThreadFactory factory = Thread.ofVirtual()
            .name(namePrefix, 1L)
            .factory();
            
        this.executor = Executors.newThreadPerTaskExecutor(factory);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual()
                .name("VirtualThreadScheduler-timer")
                .factory()
        );
        
        this.isShutdown = new AtomicBoolean(false);
        this.activeTaskCount = new AtomicInteger(0);
    }

    /**
     * 创建默认配置的调度器
     */
    public VirtualThreadScheduler() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual()
                .name("VirtualThreadScheduler-timer")
                .factory()
        );
        
        this.isShutdown = new AtomicBoolean(false);
        this.activeTaskCount = new AtomicInteger(0);
    }

    /**
     * 调度延迟执行的任务
     * @param command 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return 任务的Future对象
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        checkShutdown();
        return new DelegatingScheduledFuture<>(
            scheduler.schedule(
                () -> executeIfNotShutdown(command),
                delay,
                unit
            )
        );
    }

    /**
     * 调度延迟执行的有返回值的任务
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        checkShutdown();
        return new DelegatingScheduledFuture<>(
            scheduler.schedule(
                () -> executeIfNotShutdown(callable),
                delay,
                unit
            )
        );
    }

    /**
     * 以固定速率调度周期性任务
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit unit) {
        checkShutdown();
        return new DelegatingScheduledFuture<>(
            scheduler.scheduleAtFixedRate(
                () -> executeIfNotShutdown(command),
                initialDelay,
                period,
                unit
            )
        );
    }

    /**
     * 以固定延迟调度周期性任务
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command,
            long initialDelay,
            long delay,
            TimeUnit unit) {
        checkShutdown();
        return new DelegatingScheduledFuture<>(
            scheduler.scheduleWithFixedDelay(
                () -> executeIfNotShutdown(command),
                initialDelay,
                delay,
                unit
            )
        );
    }

    /**
     * 在非关闭状态下执行任务
     */
    private void executeIfNotShutdown(Runnable task) {
        if (!isShutdown.get()) {
            executeTask(task);
        }
    }

    /**
     * 在非关闭状态下执行有返回值的任务
     */
    private <V> V executeIfNotShutdown(Callable<V> task) throws Exception {
        if (!isShutdown.get()) {
            return executeTask(task);
        }
        throw new CancellationException("Scheduler is shutdown");
    }

    /**
     * 执行任务并管理活动任务计数
     */
    private void executeTask(Runnable task) {
        activeTaskCount.incrementAndGet();
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                handleTaskException(e);
            } finally {
                activeTaskCount.decrementAndGet();
            }
        });
    }

    /**
     * 执行有返回值的任务并管理活动任务计数
     */
    private <V> V executeTask(Callable<V> task) throws Exception {
        activeTaskCount.incrementAndGet();
        try {
            Future<V> future = executor.submit(task);
            return future.get();
        } catch (Exception e) {
            handleTaskException(e);
            throw e;
        } finally {
            activeTaskCount.decrementAndGet();
        }
    }

    /**
     * 处理任务执行过程中的异常
     */
    private void handleTaskException(Exception e) {
        // 可以添加日志记录或其他异常处理逻辑
        Thread.currentThread().getUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), e);
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

    /**
     * 包装任务集合，确保正确的任务计数管理
     */
    private <T> Collection<Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        return tasks.stream()
            .map(task -> (Callable<T>) () -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    handleTaskException(e);
                    throw e;
                } finally {
                    activeTaskCount.decrementAndGet();
                }
            })
            .collect(Collectors.toList());
    }
} 