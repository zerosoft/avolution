package com.avolution.actor.message;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.avolution.actor.core.ActorRef;

/**
 * Actor消息的 信封
 * @param
 */
public class Envelope
{

    /**
     * 消息类型 Id
     */
    private final String messageId;
    /**
     * 消息
     */
    private final Object message;
    /**
     * 发送者
     */     
    private final ActorRef sender;
    /**
     * 接收者
     */
    private final ActorRef recipient;
    /**
     * 时间戳
     */     
    private final Instant timestamp;
    /**
     * 重试次数
     */
    private final int retryCount;
    /**
     * 元数据
     */         
    private final Map<String, Object> metadata;
    /**
     * 处理过的演员集
     */     
    private final Set<String> processedActors;
    /**
     * 消息类型
     */     
    private final MessageType messageType ;
    /**
     * 信号范围
     */     
    private final SignalScope scope;
    /**
     * 优先级
     */     
    private final Priority priority;

    /**
     * 构造函数
     * @param builder
     */
    private Envelope(Builder builder) {
        this.messageId = UUID.randomUUID().toString();
        this.message = builder.message;
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.timestamp = builder.timestamp;
        this.messageType = builder.type;
        this.scope = builder.scope;
        this.priority = builder.priority;
        this.retryCount = builder.retryCount;
        this.metadata = builder.metadata;
        this.processedActors = builder.processedActors;

        // 检查必要类型参数不能没有
        if (message == null || messageType == null) {
            throw new NullPointerException("message和messageType不能为空");
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    // 基本的 getter 方法
    public String getMessageId() {
        return messageId;
    }

    public Object getMessage() {
        return message;
    }

    public ActorRef getSender() {
        return sender;
    }

    public ActorRef getRecipient() {
        return recipient;
    }

    public Instant getTimestamp() {
        return timestamp;
    }


    public int getRetryCount() {
        return retryCount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Set<String> getProcessedActors() {
        return processedActors;
    }

    public Priority getPriority() {
        return priority;
    }

    // 元数据操作
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Map<String, Object> metadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public SignalScope getScope() {
        return scope;
    }

    // 处理记录
    public void markProcessed(String actorPath) {
        processedActors.add(actorPath);
    }

    public boolean hasBeenProcessedBy(String actorPath) {
        return processedActors.contains(actorPath);
    }

    public Set<String> processedActors() {
        return Collections.unmodifiableSet(processedActors);
    }

    public boolean isSystemMessage() {
        return messageType ==MessageType.SYSTEM;
    }

    @Override
    public String toString() {
        return String.format("Envelope[id=%s, message=%s, type=%s, retry=%d, priority=%s]", messageId, message.getClass().getSimpleName(), messageType, retryCount, priority);
    }

    /**
     * Envelope builder构造器
     *
     * 该构造器用于创建Envelope对象，提供了多个方法来设置Envelope的不同属性。
     * 用户可以通过链式调用这些方法来设置Envelope的消息体、发送者、接收者、时间戳、重试次数、元数据、处理过的演员集、类型、信号范围和优先级。
     * 最终，通过build()方法创建Envelope对象。
     */
    public static class Builder{
        private Object message;

        private  ActorRef sender;
        private  ActorRef recipient;

        private  Instant timestamp;
        private  int retryCount=0;

        private  Map<String, Object> metadata=new HashMap<>();

        private  Set<String> processedActors=new HashSet<>();

        private  MessageType type;
        private  SignalScope scope=SignalScope.SINGLE;
        private  Priority priority=Priority.NORMAL;

        public Builder() {
            this.metadata = new ConcurrentHashMap<>();
            this.processedActors = ConcurrentHashMap.newKeySet();
        }

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

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder processedActors(Set<String> processedActors) {
            this.processedActors = processedActors;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder scope(SignalScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Envelope build() {
            return new Envelope(this);
        }

    }
}

