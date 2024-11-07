package com.avolution.actor.message;

import java.time.Instant;
import java.util.UUID;

import com.avolution.actor.core.ActorRef;

/**
 * 消息封装类
 */
public class Envelope<T> {
    // 消息ID
    private final String messageId;
    // 消息内容
    private final T message;
    // 发送者
    private final ActorRef<?> sender;
    // 接收者
    private final ActorRef<T> recipient;
    // 时间戳
    private final Instant timestamp;

    private final MessageType messageType;

    private final int retryCount;

    // 直接使用构造方法替代Builder
    public Envelope(T message, ActorRef<?> sender, ActorRef<T> recipient, MessageType messageType, int retryCount) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient cannot be null");
        }
        this.messageId = UUID.randomUUID().toString();
        this.message = message;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = Instant.now();
        this.messageType = messageType != null ? messageType : MessageType.NORMAL;
        this.retryCount = retryCount;
    }

    // Getters
    public String messageId() {
        return messageId;
    }

    public T getMessage() {
        return message;
    }

    public ActorRef<?> getSender() {
        return sender;
    }

    public ActorRef<T> getRecipient() {
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

    // 创建一个新Envelope，重试计数增加1
    public Envelope<T> withRetry() {
        return new Envelope<>(message, sender, recipient, messageType, retryCount + 1);
    }

    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }
}
