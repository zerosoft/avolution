package com.avolution.actor.examples;

import com.avolution.actor.core.*;
import com.avolution.actor.message.ActorMetrics;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class ActorDemo {
    public static void main(String[] args) throws Exception {
        // 创建actor系统
        ActorSystem system = ActorSystem.create("DemoSystem");
        
        try {
            // 创建主worker actor
            ActorRef worker = system.actorOf(
                Props.create(WorkerActor.class),
                "main-worker"
            );
            
            // 创建额外的worker
            worker.tellMessage(new WorkerActor.CreateWorker(), ActorRef.noSender());
            
            // 发送一些任务
            for (int i = 1; i <= 10; i++) {
                worker.tellMessage(
                    new WorkerActor.TaskMessage(i, "Task data " + i),
                    ActorRef.noSender()
                );
                
                // 每3个任务后获取度量信息
                if (i % 3 == 0) {
                    CompletionStage<Object> future = system.ask(
                        worker,
                        new WorkerActor.GetMetrics(),
                        Duration.ofSeconds(5)
                    );
                    
                    future.thenAccept(metrics -> {
                        if (metrics instanceof ActorMetrics.MetricsSnapshot snapshot) {
                            System.out.println(snapshot);
                        }
                    });
                }
                
                Thread.sleep(500);
            }
            
            // 等待所有任务完成
            Thread.sleep(2000);
            
        } finally {
            // 关闭actor系统
            system.terminate();
            system.getWhenTerminated()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
        }
    }
} 