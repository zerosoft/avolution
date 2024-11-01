package com.avolution.actor.examples;

import com.avolution.actor.core.*;
import com.avolution.actor.context.ActorContext;
import com.avolution.actor.message.ActorMetrics;
import com.avolution.actor.supervision.OneForOneStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.supervision.Directive;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 示例工作Actor，处理任务并管理子workers
 */
public class WorkerActor extends Actor {
    private final Map<String, ActorRef> workers;
    private final ActorMetrics metrics;
    private int taskCount;

    public WorkerActor() {
        this.workers = new HashMap<>();
        this.metrics = new ActorMetrics();
        this.taskCount = 0;
    }

    @Override
    protected void preStart() {
        System.out.println("WorkerActor starting at: " + getContext().path());
        // 创建初始工作者
        createWorker("worker-1");
    }

    @Override
    protected void receive(Object message) {
        long startTime = System.nanoTime();
        metrics.messageReceived();

        try {
            switch (message) {
                case TaskMessage task -> handleTask(task);
                case WorkerResult result -> handleResult(result);
                case CreateWorker create -> handleCreateWorker(create);
                case GetMetrics getMetrics -> handleGetMetrics();
                case WorkerFailed failed -> handleWorkerFailure(failed);
                default -> unhandled(message);
            }

            metrics.messageProcessed(System.nanoTime() - startTime);

        } catch (Exception e) {
            metrics.messageFailed();
            metrics.failureRecorded();
            throw e;
        }
    }

    private void unhandled(Object message) {

    }

    private void handleTask(TaskMessage task) {
        taskCount++;

        // 选择worker处理任务
        String workerId = "worker-" + (taskCount % workers.size() + 1);
        ActorRef worker = workers.get(workerId);

        if (worker != null) {
            // 使用ask模式发送任务并等待结果
            CompletionStage<Object> future = getContext().ask(task, worker);

            future.thenAccept(result ->
                    getContext().self().tellMessage(new WorkerResult(task.id(), result), worker)
            ).exceptionally(error -> {
                getContext().self().tellMessage(
                        new WorkerFailed(task.id(), error),
                        worker
                );
                return null;
            });

        } else {
            // 如果没有可用的worker，创建新的
            createWorker(workerId).tellMessage(task, getContext().self());
        }
    }

    private void handleResult(WorkerResult result) {
        System.out.printf(
                "Task %d completed with result: %s%n",
                result.taskId(),
                result.result()
        );
    }

    private ActorRef createWorker(String name) {
        Props workerProps = Props.create(TaskWorker.class)
                .withSupervisorStrategy(new OneForOneStrategy(
                        3,
                        Duration.ofMinutes(1),
                        cause -> {
                            if (cause instanceof TaskProcessingException) {
                                return Directive.RESTART;
                            }
                            return Directive.STOP;
                        }
                ));

        ActorRef worker = getContext().actorOf(workerProps, name);
        workers.put(name, worker);
        getContext().watch(worker);
        return worker;
    }

    private void handleCreateWorker(CreateWorker create) {
        createWorker("worker-" + (workers.size() + 1));
    }

    private void handleGetMetrics() {
        ActorMetrics.MetricsSnapshot snapshot = metrics.getSnapshot();
        getContext().sender().tellMessage(snapshot, getContext().self());
    }

    private void handleWorkerFailure(WorkerFailed failed) {
        System.err.printf(
                "Task %d failed with error: %s%n",
                failed.taskId(),
                failed.error().getMessage()
        );
        metrics.failureRecorded();
    }

    @Override
    protected void postStop() {
        System.out.println("WorkerActor stopping, processed " +
                taskCount + " tasks");
    }

    // 消息类
    public record TaskMessage(long id, String data) {
    }

    public record WorkerResult(long taskId, Object result) {
    }

    public record WorkerFailed(long taskId, Throwable error) {
    }

    public record CreateWorker() {
    }

    public record GetMetrics() {
    }
}