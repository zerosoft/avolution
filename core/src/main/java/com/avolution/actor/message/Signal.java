package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;

public sealed interface Signal permits PoisonPill, ReceiveTimeout, Restart, StopMessage, SupervisionMessage, SystemStopMessage, Terminated {
    void handle(AbstractActor<?> actor);
}
