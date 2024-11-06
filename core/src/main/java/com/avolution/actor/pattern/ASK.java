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
public class ASK {

    private ASK() {
    }

    public static <T, R> R ask(ActorRef<T> actorRef, T message, Duration timeout) throws Exception {
        CompletableFuture<R> future = actorRef.ask(message, timeout);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new AskTimeoutException("Ask timed out after " + timeout + " " + timeout.toMillis());
        } catch (ExecutionException e) {
            throw e; // 抛出原始异常
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("Thread was interrupted", e);
        }
    }
}
