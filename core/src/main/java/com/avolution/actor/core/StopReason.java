package com.avolution.actor.core;

/**
 * 停止原因枚举
 */
public enum StopReason {
    SELF_STOP,         // 自身调用停止
    PARENT_STOP,       // 父Actor停止
    SYSTEM_STOP,       // 系统停止
    ERROR_STOP,        // 错误导致停止
    SUPERVISION_STOP   // 监督策略导致停止
}
