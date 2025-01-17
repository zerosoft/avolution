package com.avolution.actor.pattern;


import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.avolution.actor.core.*;
import com.avolution.actor.exception.AskTimeoutException;
import com.avolution.actor.message.Envelope;

/**
 * 用于处理 ASK 模式的临时 Actor。
 * 该 Actor 会接收目标 Actor 的响应，并在超时或接收到响应后完成 CompletableFuture。
 */
public class AskActor<T, R> extends TypedActor<R> {

    private final CompletableFuture<R> future;
    private final ScheduledFuture<?> timeoutTask;

    /**
     * 构造函数，初始化 AskActor。
     *
     * @param future     用于返回结果的 CompletableFuture
     * @param timeout    超时时间
     * @param scheduler  调度器
     */
    public AskActor(CompletableFuture<R> future, Duration timeout, ScheduledExecutorService scheduler) {
        this.future = future;
        this.timeoutTask = scheduler.schedule(
                () -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(new AskTimeoutException("Ask timed out after " + timeout));
                        getContext().stop(getSelf());
                    }
                },
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 处理接收到的响应消息。
     *
     * @param response 目标 Actor 的响应
     */
    @Override
    public void onReceive(R response) {
        timeoutTask.cancel(true);
        future.complete(response);
        getContext().stop(true);
    }


    @Override
    public boolean preStop() {
        if (!future.isDone()) {
            future.completeExceptionally(new AskTimeoutException("Ask actor stopped before receiving response"));
        }
        return super.preStop();
    }

}