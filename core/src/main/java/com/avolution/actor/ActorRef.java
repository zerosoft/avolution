package com.avolution.actor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface ActorRef {

    void tellMessage(Object message, ActorRef sender);

    void forward(Object message, ActorContext context);

    String path();

    ActorRef createChild(Props props, String name);

    default <T> CompletableFuture<T> ask(Object message, long timeout, TimeUnit unit) {
        return new ASK().ask(this, message, timeout, unit);
    }
}
