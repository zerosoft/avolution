package com.avolution.actor.supervision;

import java.time.Duration;
import java.util.function.Function;

/**
 * 监督策略配置器
 * 提供静态方法来创建不同类型的监督策略（如 OneForOne 和 AllForOne）。
 */
public class SupervisorStrategyConfigurator {
    
    /**
     * 创建一个 OneForOne 监督策略
     *
     * @param maxRetries 最大重试次数
     * @param withinTime 重试窗口时间，表示在多长时间内允许重试
     * @return 返回一个 OneForOne 监督策略实例
     */
    public static SupervisorStrategy oneForOne(int maxRetries, Duration withinTime) {
        return new OneForOneStrategy(SupervisorStrategy.defaultDecider(),
                                   maxRetries,
                                   withinTime);
    }
    
    /**
     * 创建一个自定义决策函数的 OneForOne 监督策略
     *
     * @param decider    自定义决策函数，用于根据异常类型决定如何处理子Actor的失败
     * @param maxRetries 最大重试次数
     * @param withinTime 重试窗口时间，表示在多长时间内允许重试
     * @return 返回一个 OneForOne 监督策略实例
     */
    public static SupervisorStrategy oneForOne(Function<Throwable, Directive> decider,
                                             int maxRetries,
                                             Duration withinTime) {
        return new OneForOneStrategy(decider, maxRetries, withinTime);
    }
    
    /**
     * 创建一个 AllForOne 监督策略
     *
     * @param maxRetries 最大重试次数
     * @param withinTime 重试窗口时间，表示在多长时间内允许重试
     * @return 返回一个 AllForOne 监督策略实例
     */
    public static SupervisorStrategy allForOne(int maxRetries, Duration withinTime) {
        return new AllForOneStrategy(SupervisorStrategy.defaultDecider(),
                                   maxRetries,
                                   withinTime);
    }
    
    /**
     * 创建一个自定义决策函数的 AllForOne 监督策略
     *
     * @param decider    自定义决策函数，用于根据异常类型决定如何处理子Actor的失败
     * @param maxRetries 最大重试次数
     * @param withinTime 重试窗口时间，表示在多长时间内允许重试
     * @return 返回一个 AllForOne 监督策略实例
     */
    public static SupervisorStrategy allForOne(Function<Throwable, Directive> decider,
                                             int maxRetries,
                                             Duration withinTime) {
        return new AllForOneStrategy(decider, maxRetries, withinTime);
    }
} 