package com.avolution.actor;

public record MessageEnvelope(Object message, ActorRef sender) {
}
