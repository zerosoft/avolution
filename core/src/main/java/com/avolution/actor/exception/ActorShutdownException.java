package com.avolution.actor.exception;

public class ActorShutdownException extends Throwable {
    public ActorShutdownException(String failedToShutdownActor, Exception e) {
        super(failedToShutdownActor, e);
    }
}
