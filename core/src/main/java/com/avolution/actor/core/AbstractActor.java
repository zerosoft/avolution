package com.avolution.actor.core;


import com.avolution.actor.context.ActorContext;
import com.avolution.actor.lifecycle.LifecycleState;
import  com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageHandler;
import com.avolution.actor.message.MessageType;

/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public abstract class AbstractActor<T> implements ActorRef<T>, MessageHandler<T> {
    /**
     * Actor上下文
     */
    protected ActorContext context;

    private volatile Envelope<T> currentMessage;
    /**
     * Actor生命周期状态
     */
    protected LifecycleState lifecycleState = LifecycleState.NEW;

    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     */
    public abstract void onReceive(T message);

    /**
     * 获取消息发送者
     * @return
     */
    public ActorRef getSender() {
        return currentMessage.getSender();
    }

    /**
     * 获取自身引用
     * @return
     */
    public ActorRef<T> getSelf() {
        return this;
    }
    /**
     * 获取Actor上下文
     */
    public ActorContext getContext() {
        return context;
    }

    @Override
    public void tell(T message, ActorRef sender) {
        if (!isTerminated()) {
            Envelope.Builder builder = Envelope.newBuilder();
            builder.message(message);
            builder.recipient(this);
            builder.sender(sender);
            builder.retryCount(0);
            builder.messageType(MessageType.NORMAL);
            context.tell(builder.build());
        }
    }

    @Override
    public String path() {
        return context.getPath();
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

    /**
     * Actor正式初始换之前的
     * 生命周期回调方法
     */
    public void preStart() {
        // 在Actor启动前执行的操作
    }

    /**
     * 生命周期回调方法
     * 在Actor停止后执行的操作
     */
    public void postStop() {
        // 在Actor停止后执行的操作
    }

    /**
     * 生命周期回调方法
     * 在Actor重启前执行的操作
     * @param reason 重启原因
     */
    public void preRestart(Throwable reason) {
        // 在Actor重启前执行的操作
    }

    /**
     * 生命周期回调方法
     * 在Actor重启后执行的操作
     * @param reason 重启原因
     */
    public void postRestart(Throwable reason) {
        // 在Actor重启后执行的操作
    }

    /**
     * 初始化Actor上下文
     * @param context Actor上下文
     */
    public void initialize(ActorContext context) {
        this.context = context;
        this.lifecycleState = LifecycleState.STARTED;
    }

    @Override
    public void handle(Envelope<T> message) throws Exception {
        try {
            // 记录消息处理时间
            long startTime = System.nanoTime();

            currentMessage = message;

            onReceive(currentMessage.message());

            long processingTime = System.nanoTime() - startTime;

        } catch (Exception e) {
            context.handleFailure(e, message);
        } finally {
            currentMessage = null;
        }
    }
}
