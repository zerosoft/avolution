package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

public record DeadLetter(
    Object message,
    ActorRef sender,
    ActorRef recipient
) {
    @Override
    public String toString() {
        return String.format(
                "DeadLetter(%s, from=%s, to=%s)",
                message,
                sender != null ? sender.path() : "NoSender",
                recipient != null ? recipient.path() : "Unknown"
        );
    }
}
