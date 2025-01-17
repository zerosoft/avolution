package com.avolution.actor.supervision;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class OneForOneStrategy implements SupervisorStrategy {
    private final Function<Throwable, Directive> decider;
    private final int maxRetries;
    private final Duration withinTimeRange;
    private final AtomicInteger retryCount;
    private final long startTime;

    public OneForOneStrategy(Function<Throwable, Directive> decider,
                             int maxRetries,
                             Duration withinTimeRange) {
        this.decider = decider != null ? decider : SupervisorStrategy.defaultDecider();
        this.maxRetries = maxRetries;
        this.withinTimeRange = withinTimeRange;
        this.retryCount = new AtomicInteger(0);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public Directive handle(Throwable cause) {
        // 检查重试窗口
        if (System.currentTimeMillis() - startTime > withinTimeRange.toMillis()) {
            retryCount.set(0);
        }

        // 检查重试次数
        if (retryCount.incrementAndGet() > maxRetries) {
            return Directive.STOP;
        }

        return decider.apply(cause);
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public Duration getWithinTimeRange() {
        return withinTimeRange;
    }
}