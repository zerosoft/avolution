package com.avolution.actor.system.actor;


import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public interface IDeadLetterActorMessage {
    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 将普通消息转换为死信
     */
  static DeadLetter messageToDeadLetter(Envelope envelope) {
        return new DeadLetter(
                envelope.getMessage(),
                envelope.getSender().path(),
                envelope.getRecipient().path(),
                LocalDateTime.now().format(FORMATTER),
                envelope.getMessageType(),
                envelope.getRetryCount(),
                envelope.getMetadata()
        );
    }

    /**
     * 死信消息记录类
     */
    record DeadLetter(
            Object message,
            String sender,
            String recipient,
            String timestamp,
            MessageType messageType,
            int retryCount,
            Map<String, Object> metadata
    ) implements IDeadLetterActorMessage {

        public boolean isSystemMessage() {
            return messageType == MessageType.SYSTEM;
        }

        public boolean isSignalMessage() {
            return messageType == MessageType.SIGNAL;
        }

        public Optional<Object> getMetadata(String key) {
            return Optional.ofNullable(metadata.get(key));
        }

        public String getFailureReason() {
            return getMetadata("failureReason")
                    .map(Object::toString)
                    .orElse("Unknown");
        }

        @Override
        public String toString() {
            return String.format(
                    "DeadLetter[message=%s, from=%s, to=%s, at=%s, type=%s, retries=%d, reason=%s]",
                    message.getClass().getSimpleName(),
                    sender,
                    recipient,
                    timestamp,
                    messageType,
                    retryCount,
                    getFailureReason()
            );
        }
    }
}