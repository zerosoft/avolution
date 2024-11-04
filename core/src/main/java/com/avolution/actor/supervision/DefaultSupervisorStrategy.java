package com.avolution.actor.supervision;

import java.time.Duration;

public class DefaultSupervisorStrategy implements SupervisorStrategy {
    public static final DefaultSupervisorStrategy INSTANCE =
            new DefaultSupervisorStrategy();

    @Override
    public Directive handle(Throwable cause) {
        if (cause instanceof RuntimeException) {
            return Directive.RESTART;
        } else if (cause instanceof Error) {
            return Directive.STOP;
        } else {
            return Directive.ESCALATE;
        }
    }

    @Override
    public int getMaxRetries() {
        return 10;
    }

    @Override
    public Duration getWithinTimeRange() {
        return Duration.ofMinutes(1);
    }
}