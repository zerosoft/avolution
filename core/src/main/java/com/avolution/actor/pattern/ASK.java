package com.avolution.actor.pattern;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.exception.AskTimeoutException;

/**
 * Actor请求响应模式
 */
public final class ASK {
    private ASK() {}
    /**
     * 发送请求消息
     * @param actorRef 目标Actor
     * @param message 消息
     * @param timeout 超时时间
     * @return
     * @throws Exception
     */
    public static <T, R> R ask(ActorRef<T> actorRef, T message, Duration timeout) throws Exception {
        CompletableFuture<R> future = actorRef.ask(message, timeout);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new AskTimeoutException("Ask timed out after " + timeout, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException("Unexpected error during ask", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ask operation interrupted", e);
        }
    }

    /**
     * 发送请求消息
     * @param actorRef 目标Actor
     * @param message 消息
     * @param timeout 超时时间
     * @return
     */
    public static <T, R> CompletableFuture<R> askAsync(
            ActorRef<T> actorRef,
            T message,
            Duration timeout) {
        return actorRef.ask(message, timeout);
    }

    /**
     * 发送请求消息
     * @param target 目标Actor
     * @param timeout 超时时间
     * @param messageFactory 消息工厂
     * @return
     */
    public static <T, R> CompletableFuture<R> ask(
            AbstractActor<T> target,
            Duration timeout,
            Function<ActorRef<R>, T> messageFactory) {

        CompletableFuture<R> future = new CompletableFuture<>();
        ActorSystem system = target.getContext().getActorSystem();

        // 创建临时响应Actor
        Props<R> replyProps = Props.create(() -> new AbstractActor<R>() {
            private final ScheduledFuture timeoutTask;
            {
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
            }

            @Override
            public void onReceive(R response) {
                timeoutTask.cancel(true);
                future.complete(response);
            }


        });

        // 创建临时Actor并发送消息
        ActorRef<R> replyTo = system.actorOf(replyProps, "ask-" + UUID.randomUUID());
        T message = messageFactory.apply(replyTo);
        target.tell(message, replyTo);

        return future;
    }
}