package com.avolution.actor.core.strategies.impl;

import com.avolution.actor.core.strategies.StashStrategy;
import com.avolution.actor.message.Envelope;

public class DefaultStashStrategy implements StashStrategy {

    private final int maxStashSize = 100;

    @Override
    public boolean shouldStash(Envelope envelope) {
        return false; // 默认不暂存
    }

    @Override
    public boolean shouldUnstash(Envelope current) {
        return true; // 默认允许取出暂存消息
    }

    @Override
    public int getMaxStashSize() {
        return maxStashSize;
    }
}
