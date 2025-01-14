package com.avolution.actor.core.lifecycle;

/**
 * 内部生命周期钩子
 * 由 ActorContextInternalLifecycleHook 实现
 * 处理 Actor 内部状态变化
 * 负责邮箱的暂停/恢复等系统级操作
 */
public interface InternalLifecycleHook {
    /**
     * 执行内部启动逻辑
     * @return 启动是否成功
     */
    boolean executeStart();

    /**
     * 执行内部停止逻辑
     * @return 停止是否成功
     */
    boolean executeStop();

    /**
     * 执行内部重启逻辑
     * @param reason 重启原因
     * @return 重启是否成功
     */
    boolean executeRestart(Throwable reason);

    /**
     * 执行内部暂停逻辑
     * @return 暂停是否成功
     */
    boolean executeSuspend();

    /**
     * 执行内部恢复逻辑
     * @return 恢复是否成功
     */
    boolean executeResume();

    /**
     * 获取当前生命周期状态
     * @return 当前状态
     */
    LifecycleState getCurrentState();
}
