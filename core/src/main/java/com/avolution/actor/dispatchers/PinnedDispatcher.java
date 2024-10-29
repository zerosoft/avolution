package com.avolution.actor.dispatchers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PinnedDispatcher implements Dispatcher {
    private final ExecutorService executor;
    private final String name;

    public PinnedDispatcher(String name) {
        this.name = name;
        this.executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    }

    @Override
    public void dispatch(Runnable message) {
        executor.execute(message);
    }

    @Override
    public String name() {
        return name;
    }
}