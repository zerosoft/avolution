package com.avolution.actor.core.lifecycle;

import com.avolution.actor.core.context.ActorContext;
/**
 * 内部生命周期钩子
 */
public interface InternalLifecycleHook {
    /**
     * 在Actor启动之前执行
     * @param context
     */
    void aroundPreStart(ActorContext context);
    /**
     * 在Actor停止之后执行
     * @param context
     */
    void aroundPostStop(ActorContext context);
    /**
     * 在Actor重启之前执行
     * @param context
     * @param reason
     */
    void aroundPreRestart(ActorContext context, Throwable reason);
    /**
     * 在Actor重启之后执行
     * @param context
     * @param reason
     */
    void aroundPostRestart(ActorContext context, Throwable reason);
}
