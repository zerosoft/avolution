package com.avolution.actor.pattern;

import com.avolution.actor.message.Envelope;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息暂存实现
 */
public class Stash {
    private final Queue<Envelope> stash;
    private final int capacity;
    private final AtomicInteger size;

    public Stash(int capacity) {
        this.capacity = capacity;
        this.stash = new LinkedList<>();
        this.size = new AtomicInteger(0);
    }

    /**
     * 暂存消息
     */
    public void stash(Envelope message) {
        if (size.get() >= capacity) {
            throw new StashOverflowException("Stash capacity exceeded: " + capacity);
        }
        stash.offer(message);
        size.incrementAndGet();
    }

    /**
     * 取出所有暂存消息
     */
    public Queue<Envelope> unstashAll() {
        Queue<Envelope> messages = new LinkedList<>(stash);
        stash.clear();
        size.set(0);
        return messages;
    }

    /**
     * 清空暂存
     */
    public void clear() {
        stash.clear();
        size.set(0);
    }

    /**
     * 获取暂存大小
     */
    public int size() {
        return size.get();
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return size.get() == 0;
    }

    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return size.get() >= capacity;
    }

    /**
     * 获取容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 暂存溢出异常
     */
    public static class StashOverflowException extends RuntimeException {
        public StashOverflowException(String message) {
            super(message);
        }
    }
} 