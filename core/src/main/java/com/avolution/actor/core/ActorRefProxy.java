package com.avolution.actor.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * ActorRefProxy class to act as a proxy for ActorRef
 */
public class ActorRefProxy<T> implements ActorRef<T> {

    private AbstractActor<T> actor;

    public ActorRefProxy(AbstractActor<T> actor) {
        this.actor = actor;
    }

    @Override
    public void tell(T message, ActorRef sender) {
        actor.tell(message, sender);
    }

    @Override
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        return actor.ask(message, timeout);
    }

    @Override
    public String path() {
        return actor.path();
    }

    @Override
    public String name() {
        return actor.name();
    }

    @Override
    public boolean isTerminated() {
        return actor.isTerminated();
    }

    public void destroy() {
        actor = null;
    }
}
