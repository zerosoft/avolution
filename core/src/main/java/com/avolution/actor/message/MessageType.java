package com.avolution.actor.message;

/**
 * 消息类型
 */
public enum MessageType {
    SYSTEM(0),      // 系统消息优先级最高
    SIGNAL(1),      // 信号消息次之
    NORMAL(2),      // 普通消息
    DEAD_LETTER(3); // 死信优先级最低

    private final int priority;

    MessageType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
