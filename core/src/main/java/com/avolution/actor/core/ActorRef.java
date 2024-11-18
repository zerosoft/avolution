package com.avolution.actor.core;


import com.avolution.actor.message.Signal;
import com.avolution.actor.message.SignalEnvelope;
import com.avolution.actor.system.NoSender;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

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

    // 信号发送的默认实现
    default void signal(Signal signal) {
        tell((T) signal, ActorRef.noSender());
    }

    /**
     * 发送信号给Actor
     * @param signal 信号
     * @param sender 发送者
     */
    default void tell(Signal signal, ActorRef sender) {
        tell((T) signal, sender);
    }

    // 添加带参数的信号发送
    default void tell(Signal signal, Object... params) {
        SignalEnvelope envelope = new SignalEnvelope(signal, ActorRef.noSender(), (ActorRef<Signal>) this);
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                envelope.addAttachment(params[i].toString(), params[i + 1]);
            }
        }
        tell((T) envelope, ActorRef.noSender());
    }

    // 添加带参数的信号发送
    default void tell(Signal signal,ActorRef sender, Object... params) {
        SignalEnvelope envelope = new SignalEnvelope(signal, sender, (ActorRef<Signal>) this);
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                envelope.addAttachment(params[i].toString(), params[i + 1]);
            }
        }
        tell((T) envelope, ActorRef.noSender());
    }
    /**
     * 发送消息给Actor 等待返回信息
     * @param message 消息内容
     * @param timeout 超时时间
     * @return
     * @param <R>
     */
    <R> CompletableFuture<R> ask(T message, Duration timeout);

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
        return NoSender.noSender();
    }
}
