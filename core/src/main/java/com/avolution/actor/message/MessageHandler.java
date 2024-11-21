package com.avolution.actor.message;


/**
 * 消息处理器接口
 * @param
 */
public interface MessageHandler {
    /**
     * 处理消息的核心方法
     *
     * @param envelope 消息封装对象
     * @throws Exception 处理过程中的异常
     */
    void handle(Envelope envelope) throws Exception;


}
