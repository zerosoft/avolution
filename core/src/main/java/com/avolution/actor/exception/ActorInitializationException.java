package com.avolution.actor.exception;

public class ActorInitializationException extends RuntimeException {

    public ActorInitializationException(String message) {
        super(message);
    }

    public ActorInitializationException(String failedToStartActor, Exception e) {
        super(failedToStartActor, e);
    }
}
