package com.avolution.actor.exception;

public class SystemInitializationException extends RuntimeException {
    public SystemInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}