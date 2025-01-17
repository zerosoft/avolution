package com.avolution.actor.supervision;

import java.time.Duration;

/**
 * 默认的监督策略实现
 * 该策略定义了如何处理子Actor的失败，并提供了默认的最大重试次数和重试时间窗口。
 */
public class DefaultSupervisorStrategy implements SupervisorStrategy {

    /**
     * 默认的监督策略实例
     */
    public static final DefaultSupervisorStrategy INSTANCE =
            new DefaultSupervisorStrategy();

    /**
     * 处理子Actor的失败
     *
     * @param cause 导致子Actor失败的异常
     * @return 返回一个指令（Directive），指示如何处理该失败
     */
    @Override
    public Directive handle(Throwable cause) {
        if (cause instanceof RuntimeException) {
            // 如果子Actor因运行时异常失败，则重启该子Actor
            return Directive.RESTART;
        } else if (cause instanceof Error) {
            // 如果子Actor因错误（Error）失败，则停止该子Actor
            return Directive.STOP;
        } else {
            // 对于其他类型的异常，将错误升级给父Actor处理
            return Directive.ESCALATE;
        }
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    @Override
    public int getMaxRetries() {
        return 10;
    }

    /**
     * 获取重试窗口时间
     *
     * @return 重试窗口时间，表示在多长时间内允许重试
     */
    @Override
    public Duration getWithinTimeRange() {
        return Duration.ofMinutes(1);
    }
}