package com.avolution.actor.supervision;

import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorKilledException;

import java.time.Duration;
import java.util.function.Function;

public interface SupervisorStrategy {
    /**
     * 处理子Actor的失败
     */
    Directive handle(Throwable cause);
    
    /**
     * 获取最大重试次数
     */
    int getMaxRetries();
    
    /**
     * 获取重试窗口时间
     */
    Duration getWithinTimeRange();

    static Function<Throwable, Directive> defaultDecider() {
        return cause -> {
            if (cause instanceof ActorInitializationException) {
                return Directive.STOP;
            } else if (cause instanceof ActorKilledException) {
                return Directive.STOP;
            } else if (cause instanceof Exception) {
                return Directive.RESTART;
            } else {
                return Directive.ESCALATE;
            }
        };
    }
}
