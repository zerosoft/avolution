//package com.avolution.actor.examples;
//
//import com.avolution.actor.core.Actor;
//import java.util.concurrent.ThreadLocalRandom;
//
//public class TaskWorker extends Actor {
//    private int processedTasks = 0;
//
//    @Override
//    protected void preStart() {
//        System.out.println("TaskWorker starting at: " + getContext().path());
//    }
//
//    @Override
//    protected void receive(Object message) {
//        if (message instanceof WorkerActor.TaskMessage task) {
//            processedTasks++;
//
//            try {
//                // 模拟处理任务
//                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
//
//                // 随机生成失败
//                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
//                    throw new TaskProcessingException(
//                        "Failed to process task: " + task.id()
//                    );
//                }
//
//                // 返回结果
//                String result = String.format(
//                    "Processed task %d with data: %s",
//                    task.id(),
//                    task.data()
//                );
//
//                getContext().sender().tell(
//                    new WorkerActor.WorkerResult(task.id(), result),
//                    getContext().self()
//                );
//
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//
//    @Override
//    protected void postStop() {
//        System.out.println("TaskWorker stopping, processed " +
//                          processedTasks + " tasks");
//    }
//}
//
//class TaskProcessingException extends RuntimeException {
//    public TaskProcessingException(String message) {
//        super(message);
//    }
//}