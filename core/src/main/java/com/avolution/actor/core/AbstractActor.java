package com.avolution.actor.core;

import com.avolution.actor.context.ActorContext;
import com.avolution.actor.lifecycle.LifecycleState;
import com.avolution.actor.message.Envelope;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public abstract class AbstractActor<T> implements ActorRef<T> {

    protected ActorContext context;
    protected LifecycleState lifecycleState = LifecycleState.NEW;

    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     */
    protected abstract void onReceive(T message);

    /**
     * 获取Actor上下文
     */
    protected ActorContext context() {
        return context;
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        if (!isTerminated()) {
            context.tell(new Envelope(message, sender, context.self()), sender);
        }
    }

    @Override
    public <R> CompletionStage<R> ask(T message, Duration timeout) {
        return context.ask(message, timeout);
    }

    @Override
    public String path() {
        return context.self().path();
    }

    @Override
    public String name() {
        String path = path();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public boolean isTerminated() {
        return lifecycleState == LifecycleState.TERMINATED;
    }

    // 生命周期回调方法
    protected void preStart() {
    }

    protected void postStop() {
    }

    protected void preRestart(Throwable reason) {
    }

    protected void postRestart(Throwable reason) {
    }

    void initialize(ActorContext context) {
        this.context = context;
        this.lifecycleState = LifecycleState.STARTED;
    }
}