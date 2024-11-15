package com.avolution.actor.strategy;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Signal;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;

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
    default void handleMessage(Envelope<T> message, AbstractActor<T> self) throws Exception {
        if (message.getMessage() instanceof Signal signal) {
            self.handleSignal(signal);
        } else {
            self.handle(message);
        }
    }

    /**
     * 处理消息后的拦截
     */
    default void afterMessageHandle(Envelope<T> message, AbstractActor<T> self, boolean success) {}

    /**
     * 处理失败
     */
    default void handleFailure(Throwable cause, Envelope<T> message, AbstractActor<T> self) {
        Directive directive = getSupervisionStrategy().handle(cause);
        handleDirective(directive, cause, self);
    }

    /**
     * 生命周期事件
     */
    default void onPreStart(AbstractActor<T> self) {}
    default void onPreStop(AbstractActor<T> self) {}
    default void onPostStop(AbstractActor<T> self) {}
    default void onPreRestart(Throwable reason, AbstractActor<T> self) {}
    default void onPostRestart(Throwable reason, AbstractActor<T> self) {}

    /**
     * 获取监督策略
     */
    default SupervisorStrategy getSupervisionStrategy() {
        return new DefaultSupervisorStrategy();
    }

    /**
     * 处理指令
     */
    private void handleDirective(Directive directive, Throwable cause, AbstractActor<T> self) {
        switch (directive) {
            case RESUME -> self.getContext().resume();
            case RESTART -> self.getContext().restart(cause);
            case STOP -> self.getContext().stop();
            case ESCALATE -> self.getContext().escalate(cause);
        }
    }
}