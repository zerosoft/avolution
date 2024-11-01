package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;
import java.time.Instant;

public class Envelope {
    private final Object message;
    private final ActorRef sender;
    private final ActorRef recipient;
    private final Instant timestamp;
    private final boolean isSystemMessage;
    private final int retryCount;

    private Envelope(Builder builder) {
        this.message = builder.message;
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.timestamp = Instant.now();
        this.isSystemMessage = builder.isSystemMessage;
        this.retryCount = builder.retryCount;
    }

    public Object getMessage() { return message; }
    public ActorRef getSender() { return sender; }
    public ActorRef getRecipient() { return recipient; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isSystemMessage() { return isSystemMessage; }
    public int getRetryCount() { return retryCount; }

    public Envelope incrementRetry() {
        return new Builder()
            .message(message)
            .sender(sender)
            .recipient(recipient)
            .isSystemMessage(isSystemMessage)
            .retryCount(retryCount + 1)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object message;
        private ActorRef sender;
        private ActorRef recipient;
        private boolean isSystemMessage;
        private int retryCount;

        public Builder message(Object message) {
            this.message = message;
            return this;
        }

        public Builder sender(ActorRef sender) {
            this.sender = sender;
            return this;
        }

        public Builder recipient(ActorRef recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder isSystemMessage(boolean isSystemMessage) {
            this.isSystemMessage = isSystemMessage;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Envelope build() {
            return new Envelope(this);
        }
    }
} 