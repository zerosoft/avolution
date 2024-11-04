package com.avolution.actor.core;


import com.avolution.actor.context.ActorContext;
import com.avolution.actor.lifecycle.LifecycleState;
import  com.avolution.actor.message.Envelope;

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
    public abstract void onReceive(Object message);

    /**
     * 获取Actor上下文
     */
    protected ActorContext context() {
        return context;
    }

    @Override
    public void tell(T message, ActorRef sender) {
        if (!isTerminated()) {
            Envelope.Builder builder = Envelope.newBuilder();
            builder.message(message);
            context.tell(builder.build());
        }
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
        return lifecycleState == LifecycleState.STOPPED;
    }

    // 生命周期回调方法
    public void preStart() {
    }

    public void postStop() {
    }

    public void preRestart(Throwable reason) {

    }

    public void postRestart(Throwable reason) {

    }

    public void initialize(ActorContext context) {
        this.context = context;
        this.lifecycleState = LifecycleState.STARTED;
    }
}