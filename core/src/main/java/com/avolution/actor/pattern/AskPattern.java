package com.avolution.actor.pattern;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.exception.AskTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class AskPattern {
    private static final Logger logger = LoggerFactory.getLogger(AskPattern.class);

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