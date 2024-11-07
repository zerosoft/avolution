package com.avolution.actor.core;

import com.avolution.actor.message.Signal;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * ActorRefProxy class to act as a proxy for ActorRef
 */
public class ActorRefProxy<T> implements ActorRef<T> {

    private AbstractActor<T> actor;
    private volatile boolean isValid = true;

    public ActorRefProxy(AbstractActor<T> actor) {
        this.actor = actor;
        actor.setSelfRef(this);
    }

    private void checkValid() {
        if (!isValid || actor == null) {
            throw new IllegalStateException("ActorRef is no longer valid");
        }
    }

    @Override
    public void tell(T message, ActorRef sender) {
        checkValid();
        actor.tell(message, sender);
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        checkValid();
        actor.tell(signal, sender);
    }

    @Override
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        checkValid();
        return actor.ask(message, timeout);
    }

    @Override
    public String path() {
        checkValid();
        return actor.path();
    }

    @Override
    public String name() {
        checkValid();
        return actor.name();
    }

    @Override
    public boolean isTerminated() {
        return !isValid || actor == null || actor.isTerminated();
    }

    void invalidate() {
        isValid = false;
        actor = null;
    }

    public void destroy() {
        actor = null;
    }
}
