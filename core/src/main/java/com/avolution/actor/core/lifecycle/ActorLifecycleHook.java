package com.avolution.actor.core.lifecycle;


/**
 *
 * Actor生命周期回调接口
 * 由 TypedActor 实现
 * 提供生命周期事件的回调方法
 * 允许用户自定义生命周期事件的处理逻辑
 */
public interface ActorLifecycleHook {
    /**
     * Actor启动前的同步钩子
     * @return true表示可以继续，false表示终止启动
     */
    default boolean preStart() {
        return true;
    }

    /**
     * Actor停止前的同步钩子
     * @return true表示可以继续，false表示终止停止
     */
    default boolean preStop() {
        return true;
    }

    /**
     * Actor重启前的同步钩子
     * @param reason 重启原因
     * @return true表示可以继续，false表示终止重启
     */
    default boolean preRestart(Throwable reason) {
        return true;
    }

    /**
     * Actor重启后的同步钩子
     * @param reason 重启原因
     * @return true表示重启成功，false表示重启失败
     */
    default boolean postRestart(Throwable reason) {
        return true;
    }

    /**
     * Actor暂停前的同步钩子
     * @return true表示可以继续，false表示终止暂停
     */
    default boolean preSuspend() {
        return true;
    }

    /**
     * Actor恢复前的同步钩子
     * @return true表示可以继续，false表示终止恢复
     */
    default boolean preResume() {
        return true;
    }
}
