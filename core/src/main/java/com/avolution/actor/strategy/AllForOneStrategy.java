package com.avolution.actor.strategy;

import java.time.Duration;
import java.util.function.Function;

public class AllForOneStrategy implements SupervisorStrategy {

    private final int maxNrOfRetries;

    private final Duration withinTimeRange;

    private final Function<Throwable, DirectiveType> decider;

    public AllForOneStrategy(int maxNrOfRetries, Duration withinTimeRange,
                             Function<Throwable, DirectiveType> decider) {
        this.maxNrOfRetries = maxNrOfRetries;
        this.withinTimeRange = withinTimeRange;
        this.decider = decider;
    }

    @Override
    public DirectiveType decide(Throwable t) {
        return decider.apply(t);
    }
}