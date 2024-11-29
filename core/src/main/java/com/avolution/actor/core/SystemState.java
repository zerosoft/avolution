package com.avolution.actor.core;

/**
 * 系统状态
 */
public enum SystemState {
    /**
     * 新建
     */
    NEW,
    /**
     * 运行中
     */
    RUNNING, 
    /**
     * 终止中
     */
    TERMINATING, 
    /**
     * 终止
     */
    TERMINATED
}
