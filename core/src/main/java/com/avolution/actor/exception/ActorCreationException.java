package com.avolution.actor.exception;

public class ActorCreationException extends RuntimeException {
  public ActorCreationException(String message) {
    super(message);
  }
  public ActorCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
