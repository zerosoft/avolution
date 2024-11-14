package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;

import java.util.concurrent.CompletableFuture;

public final class StopMessage implements Signal {

    public final CompletableFuture<Void> future;

    public StopMessage(CompletableFuture<Void> future) {
        this.future = future;
    }

    @Override
    public void handle(AbstractActor<?> actor) {
        actor.getContext().handleStop(this);
    }
}
