package com.avolution.actor.supervision;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class OneForOneStrategy implements SupervisorStrategy {
    private final int maxRetries;
    private final Duration withinTimeRange;
    private final Function<Throwable, Directive> decider;
    private final AtomicInteger retryCount;
    private volatile long firstFailureTime;

    public OneForOneStrategy(int maxRetries, 
                           Duration withinTimeRange,
                           Function<Throwable, Directive> decider) {
        this.maxRetries = maxRetries;
        this.withinTimeRange = withinTimeRange;
        this.decider = decider;
        this.retryCount = new AtomicInteger(0);
    }

    @Override
    public Directive handle(Throwable cause) {
        long now = System.currentTimeMillis();
        
        // 检查是否需要重置重试计数
        if (firstFailureTime == 0) {
            firstFailureTime = now;
        } else if (now - firstFailureTime > withinTimeRange.toMillis()) {
            retryCount.set(0);
            firstFailureTime = now;
        }

        // 获取决策结果
        Directive directive = decider.apply(cause);
        
        // 处理重试逻辑
        if (directive == Directive.RESTART) {
            if (retryCount.incrementAndGet() > maxRetries) {
                return Directive.STOP;
            }
        }

        return directive;
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