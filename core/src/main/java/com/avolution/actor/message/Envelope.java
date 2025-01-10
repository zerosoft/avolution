package com.avolution.actor.message;

import java.time.Instant;
import java.util.UUID;

import com.avolution.actor.core.ActorRef;

/**
 * 消息包装器，携带消息的元数据和处理状态
 */
public class Envelope {
    // 消息唯一标识
    private final String id;
    // 消息内容
    private final Object message;
    // 消息类型
    private final MessageType type;
    // 消息发送者
    private final ActorRef sender;
    // 消息接收者
    private final ActorRef recipient;
    // 消息优先级
    private Priority priority;
    // 创建时间
    private final Instant createdAt;
    // 重试次数
    private int retryCount;
    // 最后一次错误
    private Throwable lastError;
    // 处理超时时间（毫秒）
    private long timeout;

    private Envelope(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.message = builder.message;
        this.type = builder.type != null ? builder.type : MessageType.NORMAL;
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.priority = builder.priority != null ? builder.priority : Priority.NORMAL;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.retryCount = builder.retryCount;
        this.lastError = builder.lastError;
        this.timeout = builder.timeout;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Object getMessage() {
        return message;
    }

    public MessageType getMessageType() {
        return type;
    }

    public ActorRef getSender() {
        return sender;
    }

    public ActorRef getRecipient() {
        return recipient;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public void setLastError(Throwable error) {
        this.lastError = error;
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isExpired() {
        return timeout > 0 && 
               Instant.now().isAfter(createdAt.plusMillis(timeout));
    }

    /**
     * Builder 模式构建器
     */
    public static class Builder {
        private String id;
        private Object message;
        private MessageType type;
        private ActorRef sender;
        private ActorRef recipient;
        private Priority priority;
        private Instant createdAt;
        private int retryCount;
        private Throwable lastError;
        private long timeout;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder message(Object message) {
            this.message = message;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
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

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder lastError(Throwable lastError) {
            this.lastError = lastError;
            return this;
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Envelope build() {
            return new Envelope(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format(
            "Envelope[id=%s, type=%s, priority=%s, retryCount=%d, message=%s]",
            id, type, priority, retryCount, message
        );
    }

    /**
     * 创建回复消息的信封
     */
    public Envelope createReply(Object replyMessage) {
        return builder()
            .message(replyMessage)
            .type(MessageType.NORMAL)
            .sender(this.recipient)
            .recipient(this.sender)
            .priority(this.priority)
            .build();
    }

    /**
     * 创建转发消息的信封
     */
    public Envelope createForward(ActorRef newRecipient) {
        return builder()
            .message(this.message)
            .type(this.type)
            .sender(this.sender)
            .recipient(newRecipient)
            .priority(this.priority)
            .build();
    }

    /**
     * 创建错误回复的信封
     */
    public Envelope createErrorReply(Throwable error) {
        return builder()
            .message(error)
            .type(MessageType.SYSTEM)
            .sender(this.recipient)
            .recipient(this.sender)
            .priority(Priority.HIGH)
            .build();
    }
}

