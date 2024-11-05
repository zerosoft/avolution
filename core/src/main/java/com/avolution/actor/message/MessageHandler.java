package com.avolution.actor.message;

/**
 * 消息处理器接口
 * @param <T> 消息类型
 */
@FunctionalInterface
public interface MessageHandler<T> {
    /**
     * 处理消息
     * @param message 消息内容
     * @throws Exception 处理过程中可能抛出的异常
     */
    void handle(Envelope<T> message) throws Exception;

}