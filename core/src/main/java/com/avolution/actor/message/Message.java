package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

/**
 * 消息包装类，包含消息内容和发送者信息
 */
public class Message<T> {
    private final T payload;
    private final ActorRef sender;
    private final long timestamp;

    public Message(T payload, ActorRef sender) {
        this.payload = payload;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取消息内容
     */
    public T getPayload() {
        return payload;
    }

    /**
     * 获取消息发送者
     */
    public ActorRef getSender() {
        return sender;
    }

    /**
     * 获取消息创建时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "payload=" + payload +
                ", sender=" + sender +
                ", timestamp=" + timestamp +
                '}';
    }
} 