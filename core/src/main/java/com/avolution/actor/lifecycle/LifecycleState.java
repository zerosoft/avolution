package com.avolution.actor.lifecycle;

/**
 * Actor的生命周期状态
 */
public enum LifecycleState {
    /**
     * 新创建状态
     */
    NEW,

    /**
     * 正在启动状态
     */
    STARTING,

    /**
     * 已启动状态
     */
    RUNNING,
    /**
     * 重新启动
     */
    RESTARTING,

    /**
     * 正在停止状态
     */
    STOPPING,
    
    /**
     * 已停止状态
     */
    STOPPED
}