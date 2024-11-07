package com.avolution.actor.system.actor;

import com.avolution.actor.message.Envelope;

public interface IDeadLetterActorMessage {

    default DeadLetter messageToDeadLetter(Envelope envelope) {
        return new DeadLetter(envelope.getMessage(), envelope.getSender().path(), envelope.getRecipient().path(),
                envelope.getTimestamp().toString(), envelope.getMessageType().toString(), envelope.getRetryCount());
    }

    record DeadLetter(Object message, String sender, String recipient, String timestamp, String messageType,
                      int retryCount) implements IDeadLetterActorMessage {

    }

}
