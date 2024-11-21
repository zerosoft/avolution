package com.avolution.actor.message;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.Map;

import com.avolution.actor.core.ActorRef;

/**
 * Actor消息的 信封
 * @param
 */
public class Envelope {

    /**
     * 消息 优先级
     */
    public enum Priority {
        HIGH(0),
        NORMAL(1),
        LOW(2);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 消息类型 Id
     */
    private final String messageId;
    private final Object message;

    private final ActorRef sender;
    private final ActorRef recipient;

    private final Instant timestamp;
    private final MessageType messageType;

    private final int retryCount;
    private final Map<String, Object> metadata;

    private final Set<String> processedActors;

    private final Priority priority;

    public Envelope(Object message, ActorRef sender, ActorRef recipient,MessageType messageType, int retryCount) {
        this(message, sender, recipient, messageType, retryCount, Priority.NORMAL);
    }

    public Envelope(Object message, ActorRef sender, ActorRef recipient, MessageType messageType, int retryCount, Priority priority) {
        validateInputs(message, recipient);
        this.messageId = UUID.randomUUID().toString();
        this.message = message;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = Instant.now();
        this.messageType = messageType != null ? messageType : MessageType.NORMAL;
        this.retryCount = retryCount;
        this.metadata = new ConcurrentHashMap<>();
        this.processedActors = ConcurrentHashMap.newKeySet();
        this.priority = priority;
    }

    private void validateInputs(Object message, ActorRef recipient) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
//        if (recipient == null) {
//            throw new IllegalArgumentException("Recipient cannot be null");
//        }
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

    public MessageType getMessageType() {
        return messageType;
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

    // 创建新的 Envelope
    public Envelope withRetry() {
        return new Envelope(message, sender, recipient, messageType, retryCount + 1, priority);
    }

    public Envelope withPriority(Priority newPriority) {
        return new Envelope(message, sender, recipient, messageType, retryCount, newPriority);
    }

    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }

    public boolean isDeadLetter() {
        return retryCount >= 3;
    }

    @Override
    public String toString() {
        return String.format("Envelope[id=%s, message=%s, type=%s, retry=%d, priority=%s]", messageId, message.getClass().getSimpleName(), messageType, retryCount, priority);
    }
}

