package com.avolution.actor.lifecycle;

/**
 * Actor上下文生命周期操作接口
 */
public interface ActorContextLifecycle {
    /**
     * 启动Actor
     * @return 启动是否成功
     */
    boolean start();

    /**
     * 优雅停止Actor
     * 等待当前消息处理完成后停止
     * @return 停止是否成功
     */
    boolean stop();

    /**
     * 立即停止Actor
     * 立即停止，不等待当前消息处理完成
     * @return 停止是否成功
     */
    boolean stopNow();

    /**
     * 重启Actor
     * @param cause 重启原因
     * @return 重启是否成功
     */
    boolean restart(Throwable cause);

    /**
     * 暂停Actor消息处理
     * @return 暂停是否成功
     */
    boolean suspend();

    /**
     * 恢复Actor消息处理
     * @return 恢复是否成功
     */
    boolean resume();



} 