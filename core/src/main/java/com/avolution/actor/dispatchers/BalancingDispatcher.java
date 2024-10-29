package com.avolution.actor.dispatchers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BalancingDispatcher implements Dispatcher {
    private final ExecutorService executor;
    private final String name;

    public BalancingDispatcher(String name, int nThreads) {
        this.name = name;
        this.executor = Executors.newFixedThreadPool(nThreads, Thread.ofVirtual().factory());
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