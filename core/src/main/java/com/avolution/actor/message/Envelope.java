package com.avolution.actor.message;

import java.time.Instant;
import java.util.UUID;

import com.avolution.actor.core.ActorRef;

/**
 * 消息封装类
 */
public class Envelope {
    // 消息ID
    private final String messageId;
    // 消息内容
    private final Object message;
    // 发送者
    private final ActorRef sender;
    // 接收者
    private final ActorRef recipient;
    // 时间戳
    private final Instant timestamp;

    private final MessageType messageType;

    private final int retryCount;

    private Envelope(Builder builder) {
        this.messageId = builder.messageId;
        this.message = builder.message;
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.timestamp = builder.timestamp;
        this.messageType = builder.messageType;
        this.retryCount = builder.retryCount;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    // Getters
    public String messageId() {
        return messageId;
    }

    public Object message() {
        return message;
    }

    public ActorRef<?> sender() {
        return sender;
    }

    public ActorRef<?> recipient() {
        return recipient;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public MessageType messageType() {
        return messageType;
    }

    public int retryCount() {
        return retryCount;
    }

    public Envelope withRetry() {
        return newBuilder()
                .from(this)
                .retryCount(this.retryCount + 1)
                .build();
    }

    public boolean isSystemMessage() {
        return messageType.equals(MessageType.SYSTEM);
    }

    public static class Builder {
        private String messageId = UUID.randomUUID().toString();
        private Object message;
        private ActorRef<?> sender;
        private ActorRef<?> recipient;
        private Instant timestamp = Instant.now();
        private MessageType messageType = MessageType.NORMAL;
        private int retryCount = 0;

        public Builder message(Object message) {
            this.message = message;
            return this;
        }

        public Builder sender(ActorRef<?> sender) {
            this.sender = sender;
            return this;
        }

        public Builder recipient(ActorRef<?> recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder messageType(MessageType type) {
            this.messageType = type;
            return this;
        }

        public Builder retryCount(int count) {
            this.retryCount = count;
            return this;
        }

        public Builder from(Envelope envelope) {
            this.messageId = envelope.messageId;
            this.message = envelope.message;
            this.sender = envelope.sender;
            this.recipient = envelope.recipient;
            this.timestamp = envelope.timestamp;
            this.messageType = envelope.messageType;
            this.retryCount = envelope.retryCount;
            return this;
        }

        public Envelope build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            if (recipient == null) {
                throw new IllegalStateException("Recipient cannot be null");
            }
            return new Envelope(this);
        }
    }

}