package com.avolution.actor.core;

import java.time.Duration;
import java.util.function.Consumer;
/**
 * Actor调度器
 */
public interface ActorScheduler extends IScheduler{

    /**
     * 调度一次性消息
     * @param key 键
     * @param delay 延迟时间
     * @param message 消息
     * @param messageHandler 消息处理器
     * @param <T>
     */
    <T> void scheduleOnce(String key, Duration delay, T message, Consumer<T> messageHandler);

    /**
     * 调度重复消息
     * @param key 键
     * @param initialDelay 初始延迟时间
     * @param interval 间隔时间
     * @param message 消息
     * @param messageHandler 消息处理器
     */
    void scheduleRepeatedly(String key, Duration initialDelay, Duration interval, Object message, Consumer<Object> messageHandler);
    /**
     * 取消调度
     * @param key 键
     */
    void cancelTimer(String key);

    /**
     * 关闭调度器
     * @throws InterruptedException
     */
    void shutdown() throws InterruptedException;
}
