package com.avolution.actor.exception;

public class ActorStopException extends RuntimeException {
  public ActorStopException(String message, Exception e) {
    super(message, e);
  }

  public ActorStopException(String message) {
    super(message);
  }
}
