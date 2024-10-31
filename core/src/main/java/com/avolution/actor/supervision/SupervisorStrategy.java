package com.avolution.actor.supervision;

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
}
