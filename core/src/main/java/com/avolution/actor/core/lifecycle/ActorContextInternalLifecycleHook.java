package com.avolution.actor.core.lifecycle;

import com.avolution.actor.core.context.ActorContext;

import java.util.concurrent.CompletableFuture;

public class ActorContextInternalLifecycleHook implements InternalLifecycleHook{

    private ActorContext actorContext;
    private ActorLifecycle actorLifecycle;

    public ActorContextInternalLifecycleHook(ActorContext actorContext, ActorLifecycle lifecycle) {
        this.actorContext=actorContext;
        this.actorLifecycle=lifecycle;
    }

    @Override
    public void executePreStart() {
        actorLifecycle.start();
        actorContext.getMailbox().resume();
    }

    @Override
    public void executePostStop() {
        actorContext.getMailbox().suspend();
        actorLifecycle.stop(new CompletableFuture<>());
    }

    @Override
    public void executePreRestart(Throwable reason) {
        actorLifecycle.restart();
    }

    @Override
    public void executePostRestart(Throwable reason) {
        actorLifecycle.resume();
    }

    @Override
    public void executeResume() {
        actorContext.getMailbox().resume();
    }

    @Override
    public void executeSuspend() {
        actorContext.getMailbox().suspend();
    }
}
