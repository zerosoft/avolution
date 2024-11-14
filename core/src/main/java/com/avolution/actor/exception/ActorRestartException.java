package com.avolution.actor.exception;

public class ActorRestartException extends Throwable {
    public ActorRestartException(String failedToRestartActor, Exception exception) {
        super(failedToRestartActor, exception);
    }
}
