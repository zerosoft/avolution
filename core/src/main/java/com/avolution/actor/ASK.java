package com.avolution.actor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ASK {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public <T> CompletableFuture<T> ask(ActorRef actorRef, Object message, long timeout, TimeUnit unit) {
        CompletableFuture<T> future = new CompletableFuture<>();

        actorRef.tellMessage(message, new ActorRef() {
            @Override
            public void tellMessage(Object message, ActorRef sender) {
                future.complete((T) message);
            }

            @Override
            public void forward(Object message, ActorContext context) {
                // No implementation needed for this example
            }

            @Override
            public String path() {
                return null;
            }

            @Override
            public ActorRef createChild(Props props, String name) {
                return null;
            }
        });

        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new TimeoutException("Timeout after " + timeout + " " + unit));
            }
        }, timeout, unit);

        return future;
    }
}
