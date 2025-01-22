package com.avolution.actor.pattern;


import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.avolution.actor.core.*;
import com.avolution.actor.exception.AskTimeoutException;

/**
 * 用于处理 ASK 模式的临时 Actor。
 * 该 Actor 会接收目标 Actor 的响应，并在超时或接收到响应后完成 CompletableFuture。
 */
public class AskActor extends TypedActor<Object> {

    private ActorSystem system;
    private Duration timeout;
    private CompletableFuture<Object> future;
    private ScheduledFuture timeoutTask;
    /**
     * 构造函数，初始化 AskActor。
     *
     * @param system      Actor 系统
     * @param future     用于返回结果的 CompletableFuture
     * @param timeout    超时时间
     */
    public AskActor(ActorSystem system, CompletableFuture<Object> future, Duration timeout) {
        this.system=system;
        this.future=future;
        this.timeout=timeout;
    }

    @Override
    public boolean preStart() {
        // 设置超时任务
        timeoutTask = system.getScheduler().schedule(
                () -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(
                                new AskTimeoutException("Ask timed out after " + timeout)
                        );
                    }
                },
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );
        return super.preStart();
    }
    /**
     * 处理接收到的响应消息。
     *
     * @param response 目标 Actor 的响应
     */
    @Override
    public void onReceive(Object response) {
        timeoutTask.cancel(true);
        future.complete(response);
    }


    @Override
    public boolean preStop() {
        if (!future.isDone()) {
            future.completeExceptionally(new AskTimeoutException("Ask actor stopped before receiving response"));
        }
        return super.preStop();
    }

}