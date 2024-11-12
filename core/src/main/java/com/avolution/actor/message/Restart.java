package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;

public final class Restart implements Signal {
    public static final Restart INSTANCE = new Restart();

    private Restart() {}

    @Override
    public void handle(AbstractActor<?> actor) {
        actor.onPreRestart(null);
        actor.onPostRestart(null);
    }
}
