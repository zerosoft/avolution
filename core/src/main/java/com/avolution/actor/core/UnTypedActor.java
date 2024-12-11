package com.avolution.actor.core;


import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.core.lifecycle.ActorLifecycleHook;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Priority;
import com.avolution.actor.message.Signal;
import com.avolution.actor.pattern.ASK;


/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public class UnTypedActor<T> implements ActorLifecycleHook,ActorRef<T> {

    Logger logger=org.slf4j.LoggerFactory.getLogger(UnTypedActor.class);
    /**
     * Actor上下文
     */
    protected ActorContext context;

    // 消息发送者
    private ActorRef sender=ActorRef.noSender();

    // 持有唯一的ActorRefProxy引用
    private LocalActorRef<T> selfRef;

    // 实现业务的TypedActor
    private TypedActor<T> typedActor;

    public UnTypedActor(TypedActor<T> typedActor) {
        this.typedActor = typedActor;
    }


    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     */
    public void onReceive(Object message) {
        try {
            logger.debug("onReceive message:{}",message);
            typedActor.onReceive((T) message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取消息发送者
     * @return
     */
    public ActorRef getSender() {
        return sender;
    }

    public void setSender(ActorRef sender) {
        this.sender = sender;
    }

    public void setSelfRef(LocalActorRef<T> ref) {
        if (this.selfRef != null) {
            throw new IllegalStateException("Self reference already set");
        }
        this.selfRef = ref;
    }

    public ActorRef<T> getSelfRef() {
        return selfRef;
    }
    /**
     * 获取Actor上下文
     */
    public ActorContext getContext() {
        return context;
    }

    public void setContext(ActorContext context) {
        if (this.context!=null){
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
        // 设置Actor上下文
        this.typedActor.setActorContext(context);
    }

    public TypedActor<T> getTypedActor() {
        return typedActor;
    }

    public void setTypedActor(TypedActor<T> typedActor) {
        if (this.typedActor != null) {
            throw new IllegalStateException("TypedActor already set");
        }
        this.typedActor = typedActor;
    }

    /**
     * 发送消息
     * @param message 消息
     * @param sender 发送者
     */
    @Override
    public void tell(T message, ActorRef sender) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!isTerminated()) {
            logger.debug("tell message:{}",message);
            if (message instanceof Envelope signalEnvelope) {
                context.tell(signalEnvelope);
                logger.debug("tell signalEnvelope:{}",signalEnvelope);
            }else {
                Envelope.Builder builder = Envelope.builder();
                builder.message(message);
                builder.sender(sender);
                builder.recipient(this.getSelfRef());
                builder.type(MessageType.NORMAL);
                builder.retryCount(0);
                Envelope envelope = builder.build();
                context.tell(envelope);
                logger.debug("tell envelope:{}",envelope);
            }

        }
    }
    /**
     * 发送信号
     * @param signal 信号
     * @param sender 发送者
     */
    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (!isTerminated()) {
            Envelope envelope = createSignalEnvelope(signal, sender);
            tell(envelope);
            logger.debug("tell signalEnvelope:{}",envelope);
        }
    }

    /**
     * 发送消息
     * @param envelope 消息
     */
    public void tell(Envelope envelope) {
        if (!isTerminated()) {
            context.tell(envelope);
            logger.debug("tell envelope:{}",envelope);
        }
    }

    /**
     * 创建信号消息
     * @param signal 信号
     * @param sender 发送者
     * @return
     */ 
    private Envelope createSignalEnvelope(Signal signal, ActorRef sender) {
        return Envelope.builder()
                .message(signal)
                .type(MessageType.SIGNAL)
                .sender(sender != null ? sender : ActorRef.noSender())
                .recipient(getSelfRef())
                .priority(Priority.HIGH)  // 信号消息优先级高
                .build();
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
        return context.getLifecycle().isTerminated();
    }

    /**
     * 发送请求消息
     * @param message 消息
     * @param timeout 超时时间
     * @return
     */
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        return ASK.ask(
                this,
                timeout,
                replyTo -> message
        );
    }

    /**
     * 发送请求消息
     * @param message 消息
     * @return
     */
    public <R> CompletableFuture<R> ask(T message) {
        return ask(message, Duration.ofSeconds(5)); // 默认5秒超时
    }


    @Override
    public void preStart() {
        typedActor.preStart();
    }

    @Override
    public void preRestart(Throwable reason) {
        typedActor.preRestart(reason);
    }

    @Override
    public void postRestart(Throwable reason) {
        typedActor.postRestart(reason);
    }

    @Override
    public void preStop() {
        typedActor.preStop();
    }

    @Override
    public void preResume() {
        typedActor.preResume();
    }

    @Override
    public void preSuspend() {
        ActorLifecycleHook.super.preSuspend();
    }
}
