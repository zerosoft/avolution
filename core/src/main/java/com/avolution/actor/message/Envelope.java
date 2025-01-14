package com.avolution.actor.message;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    // 消息作用域
    private final SignalScope scope;
    // 元数据
    private final Map<String, Object> metadata;

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
        this.scope = builder.scope != null ? builder.scope : SignalScope.SINGLE;
        this.metadata = builder.metadata != null ? new HashMap<>(builder.metadata):new HashMap<>();
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
     * 获取消息作用域
     */
    public SignalScope getScope() {
        return scope;
    }

    /**
     * 获取元数据
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 获取指定键的元数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }

    /**
     * 检查是否包含指定键的元数据
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
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
        private SignalScope scope;
        private Map<String, Object> metadata;

        public Builder() {
            this.metadata = new HashMap<>();
        }

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

        public Builder scope(SignalScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                if (this.metadata == null) {
                    this.metadata = new HashMap<>();
                }
                this.metadata.putAll(metadata);
            }
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
            "Envelope[id=%s, type=%s, priority=%s, scope=%s, retryCount=%d, message=%s]",
            id, type, priority, scope, retryCount, message
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
            .scope(this.scope)
            .metadata(this.metadata)  // 保持元数据
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
            .scope(this.scope)
            .metadata(this.metadata)  // 保持元数据
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
            .scope(SignalScope.SINGLE)  // 错误消息通常是本地的
            .metadata("error.original.message", this.message)  // 记录原始消息
            .metadata("error.timestamp", Instant.now())
            .build();
    }
}

