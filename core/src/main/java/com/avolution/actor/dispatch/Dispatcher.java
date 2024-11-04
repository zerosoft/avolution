package com.avolution.actor.dispatch;

import com.avolution.actor.message.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Dispatcher {
    private Logger logger= LoggerFactory.getLogger(Dispatcher.class);

    private final ExecutorService executor;
    private final Map<String, TaskQueue> taskQueues;
    private volatile boolean isShutdown;

    public Dispatcher() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.taskQueues = new ConcurrentHashMap<>();
        this.isShutdown = false;
    }

    public void dispatch(String actorPath, Runnable processingTask) {
        if (isShutdown) {
            return;
        }
        TaskQueue taskQueue = taskQueues.computeIfAbsent(actorPath, k -> new TaskQueue());
        taskQueue.addTask(processingTask);
    }

    public void shutdown() {
        isShutdown = true;
        taskQueues.values().forEach(TaskQueue::clear);
        executor.shutdown();
    }

    private class TaskQueue {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean isProcessing = new AtomicBoolean(false);

        public void addTask(Runnable task) {
            tasks.offer(task);
            if (isProcessing.compareAndSet(false, true)) {
                processNextTask();
            }
        }

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

        public void clear() {
            tasks.clear();
        }
    }
}
