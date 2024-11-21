package com.avolution.actor.core.lifecycle;

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
     * 暂停挂起状态
     */
    SUSPENDED,
    /**
     * 已停止状态
     */
    STOPPED,

    /**
     * 已停止（失败）状态
     */
    FAILED
}