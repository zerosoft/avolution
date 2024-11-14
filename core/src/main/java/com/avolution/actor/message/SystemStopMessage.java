package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;

import java.util.concurrent.CompletableFuture;

public final class SystemStopMessage implements Signal{

   public final CompletableFuture<Void> future;

    public SystemStopMessage(CompletableFuture<Void> future) {
        this.future = future;
    }

    @Override
    public void handle(AbstractActor<?> actor) {
        actor.getContext().handleStop(new StopMessage(future));
    }
}
