package com.avolution.actor.exception;

public class ActorSystemCreationException extends Throwable {
    public ActorSystemCreationException(String systemActorsInitializationFailed, Exception e) {
        super(systemActorsInitializationFailed, e);
    }

    public ActorSystemCreationException(String format) {
        super(format);
    }
}
