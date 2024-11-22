package com.avolution.actor.core.lifecycle;

import com.avolution.actor.core.context.ActorContext;

public interface InternalLifecycleHook {
    // 系统内部使用的生命周期钩子
    void aroundPreStart(ActorContext context);
    void aroundPostStop(ActorContext context);
    void aroundPreRestart(ActorContext context, Throwable reason);
    void aroundPostRestart(ActorContext context, Throwable reason);
}
