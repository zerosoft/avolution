package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;

/**
 * 系统消息 - 用于停止Actor的特殊消息
 * 当Actor收到此消息时，将触发其停止流程
 */
public final class PoisonPill implements Signal {

    public static final PoisonPill INSTANCE = new PoisonPill();

    private PoisonPill() {}

    @Override
    public void handle(AbstractActor<?> actor) {
        actor.shutdown();
    }
}
