package com.avolution.actor.supervision;

import java.time.Duration;
import java.util.function.Function;

/**
 * AllForOne 监督策略
 * 当某个子Actor失败时，该策略会应用于所有子Actor。
 */
public class AllForOneStrategy implements SupervisorStrategy {
    // 决策函数，用于根据异常类型决定如何处理子Actor的失败
    private final Function<Throwable, Directive> decider;
    // 最大重试次数
    private final int maxRetries;
    // 重试窗口时间，表示在多长时间内允许重试
    private final Duration withinTimeRange;

    /**
     * 构造函数
     *
     * @param decider        自定义决策函数，用于根据异常类型决定如何处理子Actor的失败
     * @param maxRetries     最大重试次数
     * @param withinTimeRange 重试窗口时间，表示在多长时间内允许重试
     */
    public AllForOneStrategy(Function<Throwable, Directive> decider,
                             int maxRetries,
                             Duration withinTimeRange) {
        this.decider = decider != null ? decider : SupervisorStrategy.defaultDecider();
        this.maxRetries = maxRetries;
        this.withinTimeRange = withinTimeRange;
    }

    /**
     * 处理子Actor的失败
     *
     * @param cause 导致子Actor失败的异常
     * @return 返回一个指令（Directive），指示如何处理该失败
     */
    @Override
    public Directive handle(Throwable cause) {
        Directive directive = decider.apply(cause);

        // 如果是重启指令，则改为重启所有子Actor
        if (directive == Directive.RESTART) {
            return Directive.RESTART_ALL;
        }

        return directive;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 获取重试窗口时间
     *
     * @return 重试窗口时间，表示在多长时间内允许重试
     */
    @Override
    public Duration getWithinTimeRange() {
        return withinTimeRange;
    }
}