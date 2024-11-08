package com.avolution.actor.system.actor;

import com.avolution.actor.message.Envelope;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface IDeadLetterActorMessage {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    default DeadLetter messageToDeadLetter(Envelope envelope) {
        return new DeadLetter(
                envelope.getMessage(),
                envelope.getSender().path(),
                envelope.getRecipient().path(),
                LocalDateTime.now().format(FORMATTER),
                envelope.getMessageType().toString(),
                envelope.getRetryCount()
        );
    }

    record DeadLetter(
            Object message,
            String sender,
            String recipient,
            String timestamp,
            String messageType,
            int retryCount
    ) implements IDeadLetterActorMessage {
        @Override
        public String toString() {
            return String.format(
                    "DeadLetter[message=%s, from=%s, to=%s, at=%s, type=%s, retries=%d]",
                    message, sender, recipient, timestamp, messageType, retryCount
            );
        }
    }

}
