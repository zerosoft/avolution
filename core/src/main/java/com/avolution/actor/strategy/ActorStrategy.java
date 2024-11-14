package com.avolution.actor.strategy;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;

/**
 * Actor策略
 * @param <T>
 */
public interface ActorStrategy<T> {
    /**
     * 处理消息前的拦截
     */
    default void beforeMessageHandle(Envelope<T> message, AbstractActor<T> self) {}

    /**
     * 处理消息
     */
    void handleMessage(Envelope<T> message, AbstractActor<T> self);

    /**
     * 处理消息后的拦截
     */
    default void afterMessageHandle(Envelope<T> message, AbstractActor<T> self, boolean success) {}

    /**
     * 处理失败
     */
    void handleFailure(Throwable cause, Envelope<T> message, AbstractActor<T> self);

    /**
     * 生命周期事件
     */
    default void onPreStart(AbstractActor<T> self) {}
    default void onPreStop(AbstractActor<T> self) {}
    default void onPostStop(AbstractActor<T> self) {}
    default void onPreRestart(Throwable reason, AbstractActor<T> self) {}
    default void onPostRestart(Throwable reason, AbstractActor<T> self) {}
}