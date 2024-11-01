package com.avolution.actor.config.strategies;

import com.avolution.actor.message.Envelope;

public interface StashStrategy {

    boolean shouldStash(Envelope envelope);

    boolean shouldUnstash(Envelope current);

    int getMaxStashSize();
}
