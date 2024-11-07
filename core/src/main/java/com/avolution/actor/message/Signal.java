package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;

public sealed interface Signal permits Restart, PoisonPill {
    void handle(AbstractActor<?> actor);
}
