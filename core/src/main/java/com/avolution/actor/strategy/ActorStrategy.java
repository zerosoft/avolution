package com.avolution.actor.strategy;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;

/**
 * Actor策略
 */
public interface ActorStrategy {
    // 消息处理拦截
    default void beforeMessageHandle(Envelope message, AbstractActor<?> actor) {}
    default void afterMessageHandle(Envelope message, AbstractActor<?> actor, boolean success) {}

    // 生命周期事件
    default void onPreStart(AbstractActor<?> actor) {}
    default void onPreStop(AbstractActor<?> actor) {}
    default void onPostStop(AbstractActor<?> actor) {}
    default void onPreRestart(AbstractActor<?> actor, Throwable reason) {}
    default void onPostRestart(AbstractActor<?> actor, Throwable reason) {}

    // 错误处理
    default void handleFailure(Throwable cause, Envelope message, AbstractActor<?> actor) {
        Directive directive = getSupervisionStrategy().handle(cause);
        handleDirective(directive, cause, actor);
    }

    // 监督策略
    default SupervisorStrategy getSupervisionStrategy() {
        return new DefaultSupervisorStrategy();
    }

    // 处理监督指令
    private void handleDirective(Directive directive, Throwable cause, AbstractActor<?> actor) {
        switch (directive) {
        }
    }
}