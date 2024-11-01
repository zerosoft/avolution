package com.avolution.actor.config.strategies;

public enum OverflowStrategy {
    DROP_NEW,       // 丢弃新消息
    DROP_OLD,       // 丢弃旧消息
    DROP_BUFFER,    // 清空缓冲区
    BLOCK           // 阻塞等待
}
