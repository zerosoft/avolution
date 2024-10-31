package com.avolution.actor.core;

import com.avolution.actor.context.ActorContext;

public abstract class Actor {
    private ActorContext context;

    void setContext(ActorContext context) {
        this.context = context;
    }

    protected ActorContext getContext() {
        return context;
    }

    protected void preStart() {
    }

    protected void postStop() {
    }

    protected void preRestart(Throwable reason) {
    }

    protected void postRestart(Throwable reason) {
    }

    protected abstract void receive(Object message);
}