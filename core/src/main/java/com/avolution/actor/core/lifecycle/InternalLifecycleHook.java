package com.avolution.actor.core.lifecycle;

/**
 * 内部生命周期钩子
 */
public interface InternalLifecycleHook {
    /**
     * 执行预启动
     */ 
    void executePreStart();

    /**
     * 执行后停止
     */
    void executePostStop();

    /**
     * 执行预重启
     * @param reason 导致重启的原因
     */ 
    void executePreRestart(Throwable reason);

    /**
     * 执行后重启
     * @param reason 导致重启的原因
     */
    void executePostRestart(Throwable reason);

    /**
     * 执行恢复
     */
    void executeResume();

    /**
     * 执行暂停
     */
    void executeSuspend();
}
