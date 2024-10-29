package com.avolution.actor;

public interface ActorRef {
    void tell(Object message, ActorRef sender);

    void forward(Object message, ActorContext context);

    String path();
}
