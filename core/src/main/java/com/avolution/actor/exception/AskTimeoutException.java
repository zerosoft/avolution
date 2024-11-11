package com.avolution.actor.exception;

import java.util.concurrent.TimeoutException;

public class AskTimeoutException extends RuntimeException {
    public AskTimeoutException(String message, TimeoutException e) {
        super(message);
    }


    public AskTimeoutException(String message) {
        super(message);
    }
}
