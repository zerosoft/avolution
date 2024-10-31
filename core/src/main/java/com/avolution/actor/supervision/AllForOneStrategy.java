package com.avolution.actor.supervision;

import java.time.Duration;
import java.util.function.Function;

public class AllForOneStrategy implements SupervisorStrategy {
    private final OneForOneStrategy delegate;

    public AllForOneStrategy(int maxRetries,
                           Duration withinTimeRange,
                           Function<Throwable, Directive> decider) {
        this.delegate = new OneForOneStrategy(maxRetries, withinTimeRange, decider);
    }

    @Override
    public Directive handle(Throwable cause) {
        Directive directive = delegate.handle(cause);
        // All-For-One策略会影响所有子Actor
        return directive == Directive.RESTART ? Directive.RESTART_ALL : directive;
    }

    @Override
    public int getMaxRetries() {
        return delegate.getMaxRetries();
    }

    @Override
    public Duration getWithinTimeRange() {
        return delegate.getWithinTimeRange();
    }
} 