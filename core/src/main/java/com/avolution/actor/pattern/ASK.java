package com.avolution.actor.pattern;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.avolution.actor.core.*;
import com.avolution.actor.exception.AskTimeoutException;
import com.avolution.actor.system.actor.SystemGuardianActorMessage;

/**
 * Actor请求响应模式
 */
public final class ASK {
    public static final String ASK = "ask";

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
            throw new RuntimeException("Unexpected error during " + ASK, e);
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
    public static <T, R> CompletableFuture<R> ask(UnTypedActor<T> target,Duration timeout,Function<ActorRef<R>, T> messageFactory) {

        CompletableFuture<R> resultFuture = new CompletableFuture<>();

        ActorSystem system = target.getContext().getActorSystem();

        Props<Object> replyProps = Props.create(AskActor.class, system, resultFuture, timeout);

        CompletableFuture<ActorRef> futureCreate = new CompletableFuture<>();
        // 通过用户守护者创建Actor
        String name = ASK + "-" + UUID.randomUUID();
        system.getSystemGuardian().tell(new SystemGuardianActorMessage.CreateAskActor(replyProps, name, futureCreate), ActorRef.noSender());

        ActorRef<Object> replyTo = null;
        try {
            replyTo = futureCreate.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

        // 创建临时Actor并发送消息
        T message = messageFactory.apply((ActorRef<R>) replyTo);
        target.tell(message, replyTo);

        return resultFuture;
    }
}