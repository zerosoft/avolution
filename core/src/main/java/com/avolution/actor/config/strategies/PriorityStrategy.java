package com.avolution.actor.config.strategies;

import com.avolution.actor.message.Envelope;

public interface PriorityStrategy {

    int getPriority(Envelope envelope);

    boolean isHighPriority(Envelope envelope);

    int getMaxPriority();
}
