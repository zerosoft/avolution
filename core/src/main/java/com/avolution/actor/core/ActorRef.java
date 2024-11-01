package com.avolution.actor.core;

import com.avolution.actor.impl.DeadLetterActorRef;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * Actor引用接口
 * 提供与Actor交互的最小必要方法集
 */
public interface ActorRef<T> {
    /**
     * 发送消息给Actor
     *
     * @param message 消息内容
     * @param sender 消息发送者
     */
    void tell(T message, ActorRef sender);

    /**
     * 发送消息并等待响应
     *
     * @param message 消息内容
     * @param timeout 超时时间
     * @return 响应的Future
     */
    <T> CompletionStage<T> ask(T message, Duration timeout);

    /**
     * 获取Actor的路径
     *
     * @return actor路径
     */
    String path();

    /**
     * 获取Actor的名称
     *
     * @return actor名称
     */
    String name();

    /**
     * 检查Actor是否已终止
     *
     * @return 是否已终止
     */
    boolean isTerminated();

    /**
     * 获取空的发送者引用
     *
     * @return 空发送者
     */
    static ActorRef noSender() {
        return DeadLetterActorRef.INSTANCE;
    }
}