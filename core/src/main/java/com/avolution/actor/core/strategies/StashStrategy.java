package com.avolution.actor.core.strategies;

import com.avolution.actor.message.Envelope;

public interface StashStrategy {

    boolean shouldStash(Envelope envelope);

    boolean shouldUnstash(Envelope current);

    int getMaxStashSize();
}
