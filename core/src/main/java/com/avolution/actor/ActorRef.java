package com.avolution.actor;

public interface ActorRef {

    void tellMessage(Object message, ActorRef sender);

    void forward(Object message, ActorContext context);

    String path();
}
