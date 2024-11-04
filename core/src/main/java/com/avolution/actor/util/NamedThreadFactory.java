package com.avolution.actor.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicLong count = new AtomicLong(1);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(prefix + count.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}