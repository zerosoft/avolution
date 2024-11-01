package com.avolution.actor.config.strategies.impl;

import com.avolution.actor.config.strategies.PriorityStrategy;
import com.avolution.actor.message.Envelope;

public class DefaultPriorityStrategy implements PriorityStrategy {
    private final int maxPriority = 10;

    @Override
    public int getPriority(Envelope envelope) {
        return 0; // 默认优先级
    }

    @Override
    public boolean isHighPriority(Envelope envelope) {
        return envelope.isSystemMessage(); // 系统消息为高优先级
    }

    @Override
    public int getMaxPriority() {
        return maxPriority;
    }
}