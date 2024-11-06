package com.avolution.actor.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 代理ScheduledFuture
 * @param <V>
 */
public class DelegatingScheduledFuture<V> implements ScheduledFuture<V> {

    private final ScheduledFuture<V> delegate;

    public DelegatingScheduledFuture(ScheduledFuture<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return delegate.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed other) {
        return delegate.compareTo(other);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }
} 