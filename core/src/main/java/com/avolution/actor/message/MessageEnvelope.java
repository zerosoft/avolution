package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

public record MessageEnvelope(Object message, ActorRef sender) {
}
