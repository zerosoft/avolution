package com.avolution.actor.core;

import com.avolution.actor.impl.DeadLetterActorRef;

public interface ActorRef {
    void tellMessage(Object message, ActorRef sender);

    String path();

    void stop();

    static ActorRef noSender() {
        return DeadLetterActorRef.INSTANCE;
    }
}