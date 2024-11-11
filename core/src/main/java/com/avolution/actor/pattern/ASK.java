package com.avolution.actor.pattern;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.exception.AskTimeoutException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Actor请求响应模式
 */
public final class ASK {
    private ASK() {}

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

    public static <T, R> CompletableFuture<R> askAsync(
            ActorRef<T> actorRef,
            T message,
            Duration timeout) {
        return actorRef.ask(message, timeout);
    }
}