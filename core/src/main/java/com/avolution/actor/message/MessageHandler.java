package com.avolution.actor.message;

import java.util.concurrent.CompletableFuture;

/**
 * 消息处理器接口
 * @param <T> 消息类型
 */
public interface MessageHandler<T> {
    /**
     * 处理消息的核心方法
     *
     * @param envelope 消息封装对象
     * @throws Exception 处理过程中的异常
     */
    void handle(Envelope<T> envelope) throws Exception;

    /**
     * 处理系统信号
     *
     * @param signal 系统信号
     * @throws Exception 处理过程中的异常
     */
    default void handleSignal(Signal signal) throws Exception {
        // 默认实现为空
    }

    /**
     * 处理未知消息类型
     *
     * @param message 未知消息
     */
    default void unhandled(T message) {
        // 默认实现记录警告日志
    }

    /**
     * 处理死信消息
     *
     * @param envelope 死信消息封装对象
     */
    default void handleDeadLetter(Envelope<?> envelope) {
        // 默认实现为空
    }

    /**
     * 获取当前正在处理的消息
     *
     * @return 当前消息封装对象
     */
    Envelope<T> getCurrentMessage();

    /**
     * 中断当前消息处理
     */
    void interruptCurrentProcessing();

    /**
     * 等待当前消息处理完成
     *
     * @return 完成的Future
     */
    CompletableFuture<Void> waitForMessageProcessing();
}
