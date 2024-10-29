package com.avolution.actor;

import java.util.Optional;

public abstract class Actor {
    private ActorContext context;
    private ActorRef self;

    public void setContext(ActorContext context, ActorRef self) {
        this.context = context;
        this.self = self;
    }

    protected ActorContext context() {
        return context;
    }

    protected ActorRef self() {
        return self;
    }

    protected ActorRef sender() {
        return context.sender();
    }

    protected void preStart() {
    }

    protected void postStop() {
    }

    protected void preRestart(Throwable reason, Optional<Object> message) {
    }

    protected void postRestart(Throwable reason) {
    }

    protected abstract void receive(Object message);
}
