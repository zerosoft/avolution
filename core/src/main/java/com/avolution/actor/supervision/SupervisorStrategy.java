package com.avolution.actor.supervision;

import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorKilledException;

import java.time.Duration;
import java.util.function.Function;

/**
 * 监督策略接口
 * 定义如何处理子Actor的失败，并提供重试机制和重试时间窗口。
 */
public interface SupervisorStrategy {

    /**
     * 处理子Actor的失败
     *
     * @param cause 导致子Actor失败的异常
     * @return 返回一个指令（Directive），指示如何处理该失败
     */
    Directive handle(Throwable cause);

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    int getMaxRetries();

    /**
     * 获取重试窗口时间
     *
     * @return 重试窗口时间，表示在多长时间内允许重试
     */
    Duration getWithinTimeRange();

    /**
     * 默认的决策函数
     * 根据异常类型决定如何处理子Actor的失败。
     *
     * @return 返回一个函数，该函数根据异常类型返回相应的指令（Directive）
     */
    static Function<Throwable, Directive> defaultDecider() {
        return cause -> {
            if (cause instanceof ActorInitializationException) {
                // 如果子Actor初始化失败，则停止该子Actor
                return Directive.STOP;
            } else if (cause instanceof ActorKilledException) {
                // 如果子Actor被强制终止，则停止该子Actor
                return Directive.STOP;
            } else if (cause instanceof Exception) {
                // 如果子Actor因普通异常失败，则重启该子Actor
                return Directive.RESTART;
            } else {
                // 对于其他类型的异常，将错误升级给父Actor处理
                return Directive.ESCALATE;
            }
        };
    }
}