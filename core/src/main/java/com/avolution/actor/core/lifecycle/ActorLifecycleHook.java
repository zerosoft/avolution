package com.avolution.actor.core.lifecycle;

import com.avolution.actor.message.Envelope;

import java.util.function.Consumer;

/**
 *
 * Actor生命周期回调接口
 *
 */
public interface ActorLifecycleHook {
    // 用户可重写的生命周期钩子
    default void preStart() {}
    default void postStart() {}
    default void preStop() {}
    default void postStop() {}
    default void preRestart(Throwable reason) {}
    default void postRestart(Throwable reason) {}
    default void aroundReceive(Envelope message, Consumer<Envelope> next) {
        next.accept(message);
    }
}
