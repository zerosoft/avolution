package com.avolution.actor.message;
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
