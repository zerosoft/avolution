package com.avolution.actor.message;

/**
 * 消息类型
 */
public enum MessageType {
    // 普通消息
    NORMAL,
    // 系统消息
    SYSTEM,
    // 监管消息
    SUPERVISION,
    // 退出消息
    POISON_PILL,
    // 监视消息
    WATCH,
    // 取消监视消息
    UNWATCH
}
