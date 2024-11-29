package com.avolution.actor.dispatch;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 调度器
 */
public class Dispatcher {
    private Logger logger= LoggerFactory.getLogger(Dispatcher.class);
    /**
     * 执行器
     */
    private final ExecutorService executor;
    /**
     * 任务队列
     */
    private final Map<String, TaskQueue> taskQueues;
    /**
     * 是否关闭
     */
    private volatile boolean isShutdown;

    public Dispatcher() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.taskQueues = new ConcurrentHashMap<>();
        this.isShutdown = false;
    }

    /**
     * 调度任务
     * @param actorPath 路径
     * @param processingTask 任务
     */
    public void dispatch(String actorPath, Runnable processingTask) {
        if (isShutdown) {
            return;
        }
        TaskQueue taskQueue = taskQueues.computeIfAbsent(actorPath, k -> new TaskQueue());
        taskQueue.addTask(processingTask);
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        isShutdown = true;
        taskQueues.values().forEach(TaskQueue::clear);
        executor.shutdown();
    }

    /**
     * 任务队列
     */
    private class TaskQueue {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean isProcessing = new AtomicBoolean(false);

        public void addTask(Runnable task) {
            tasks.offer(task);
            if (isProcessing.compareAndSet(false, true)) {
                processNextTask();
            }
        }

        /**
         * 处理下一个任务
         */
        private void processNextTask() {
            executor.execute(() -> {
                try {
                    Runnable task;
                    while ((task = tasks.poll()) != null) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            // 记录错误但继续处理队列
                            logger.error("Error processing task", e);
                        }
                    }
                } finally {
                    isProcessing.set(false);
                    // 如果在处理过程中有新任务加入，确保继续处理
                    if (!tasks.isEmpty() && isProcessing.compareAndSet(false, true)) {
                        processNextTask();
                    }
                }
            });
        }

        /**
         * 清空任务队列
         */
        public void clear() {
            tasks.clear();
        }
    }
}
