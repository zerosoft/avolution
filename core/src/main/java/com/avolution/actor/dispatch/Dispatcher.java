package com.avolution.actor.dispatch;

import com.avolution.actor.config.DispatcherConfig;
import com.avolution.actor.core.ActorRef;

import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 基于虚拟线程的Actor调度器
 */
public class Dispatcher {
    private final ExecutorService executor;
    private final DispatcherConfig config;
    private final DispatcherMetrics metrics;
    private final Map<ActorRef, AtomicBoolean> suspendedActors;
    private final AtomicBoolean isShutdown;

    // 用于限制并发任务数量的信号量
    private final Semaphore concurrencyLimiter;

    private final Map<ActorRef, Queue<Runnable>> messageQueues;
    private final Map<ActorRef, AtomicBoolean> processingFlags;

    public Dispatcher(DispatcherConfig config) {
        this.config = config;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.metrics = new DispatcherMetrics();
        this.suspendedActors = new ConcurrentHashMap<>();
        this.isShutdown = new AtomicBoolean(false);
        this.concurrencyLimiter = new Semaphore(config.getMaxConcurrency());
        this.messageQueues = new ConcurrentHashMap<>();
        this.processingFlags = new ConcurrentHashMap<>();
    }

    /**
     * 执行Actor任务
     */
    public void execute(ActorRef actor, Runnable task) {
        if (isShutdown.get()) {
            throw new RejectedExecutionException("Dispatcher is shutdown");
        }

        AtomicBoolean suspended = suspendedActors.get(actor);
        if (suspended != null && suspended.get()) {
            return;
        }

        // 获取或创建actor的消息队列
        Queue<Runnable> queue = messageQueues.computeIfAbsent(actor, k -> new ConcurrentLinkedQueue<>());
        // 添加任务到队列
        queue.offer(task);

        // 尝试处理队列
        processActorQueue(actor);
    }

    /**
     * 异步执行并返回结果
     */
    public <T> CompletableFuture<T> executeAsync(
            ActorRef actor,
            Callable<T> task
    ) {
        CompletableFuture<T> future = new CompletableFuture<>();

        execute(actor, () -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    /**
     * 暂停Actor的消息处理
     */
    public void suspend(ActorRef actor) {
        suspendedActors.computeIfAbsent(
                actor,
                k -> new AtomicBoolean()
        ).set(true);
    }

    /**
     * 恢复Actor的消息处理
     */
    public void resume(ActorRef actor) {
        AtomicBoolean suspended = suspendedActors.get(actor);
        if (suspended != null) {
            suspended.set(false);
        }
    }

    /**
     * 优雅关闭调度器
     */
    public boolean shutdown(Duration timeout) {
        if (isShutdown.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                return executor.awaitTermination(
                        timeout.toMillis(),
                        TimeUnit.MILLISECONDS
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * 立即关闭调度器
     */
    public void shutdownNow() {
        if (isShutdown.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }

    /**
     * 获取度量信息
     */
    public DispatcherMetrics getMetrics() {
        return metrics;
    }

    private void handleError(ActorRef actor, Throwable error) {
        System.err.println("Error in actor " + actor.path() + ": " + error);
    }

    private void handleRejection(ActorRef actor, Runnable task) {
        System.err.println("Task rejected for actor " + actor.path());
    }

    private void processActorQueue(ActorRef actor) {
        // 确保同一时间只有一个线程在处理actor的消息队列
        AtomicBoolean processing = processingFlags.computeIfAbsent(actor, k -> new AtomicBoolean(false));

        if (processing.compareAndSet(false, true)) {
            try {
                if (!concurrencyLimiter.tryAcquire(config.getTaskTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                    processing.set(false);
                    metrics.taskRejected();
                    handleRejection(actor, null);
                    return;
                }

                CompletableFuture.runAsync(() -> processMessages(actor), executor);
            } catch (InterruptedException e) {
                processing.set(false);
                Thread.currentThread().interrupt();
                metrics.taskRejected();
                handleRejection(actor, null);
            }
        }
    }

    private void processMessages(ActorRef actor) {
        try {
            Queue<Runnable> queue = messageQueues.get(actor);
            Runnable task;

            while ((task = queue.poll()) != null) {
                long startTime = System.nanoTime();
                try {
                    metrics.taskStarted();
                    task.run();
                    metrics.taskCompleted(System.nanoTime() - startTime);
                } catch (Throwable t) {
                    metrics.taskFailed();
                    handleError(actor, t);
                }
            }
        } finally {
            concurrencyLimiter.release();
            processingFlags.get(actor).set(false);

            // 如果队列中还有消息，继续处理
            if (!messageQueues.get(actor).isEmpty()) {
                processActorQueue(actor);
            }
        }
    }
}