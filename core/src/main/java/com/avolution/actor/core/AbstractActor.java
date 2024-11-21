package com.avolution.actor.core;


import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.message.*;
import com.avolution.actor.pattern.AskPattern;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public abstract class AbstractActor<T> implements ActorRef<T> {
    Logger logger=org.slf4j.LoggerFactory.getLogger(AbstractActor.class);

    /**
     * Actor上下文
     */
    protected ActorContext context;

    // 持有唯一的ActorRefProxy引用
    private LocalActorRef<T> selfRef;

    private ActorRef sender=ActorRef.noSender();
    /**
     * 消息处理器
     */
    private final Map<Class<?>, Consumer<Object>> handlers = new HashMap<>();


    private void registerHandlers() {
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnReceive.class)) {
                Class<?> messageType = method.getAnnotation(OnReceive.class).value();
                if (method.getParameterCount() == 1 && messageType.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    handlers.put(messageType, message -> invokeHandler(method, message));
                }
            }
        }
    }

    private void invokeHandler(Method method, Object message) {
        try {
            method.invoke(this, message);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            logger.error("Error invoking message handler for {}: {}", message.getClass().getSimpleName(), cause.getMessage());
        } catch (Exception e) {
            logger.error("Error invoking message handler", e);
        }
    }


    public void initialize() {
        try {
            // 1. 注册消息处理器
            registerHandlers();

            logger.debug("Actor initialized: {}", context.getPath());
        } catch (Exception e) {
            logger.error("Failed to initialize actor: {}", context.getPath(), e);
            throw new ActorInitializationException("Actor initialization failed", e);
        }
    }

    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     */
    public void onReceive(T message) {
        Consumer<Object> handler = handlers.get(message.getClass());

        if (handler != null) {
            handler.accept(message);
        } else {
            unhandled(message);
        }
    }

    public void unhandled(T message) {
        System.out.println("Unhandled message: " + message);
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

    @Override
    public void tell(T message, ActorRef sender) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!isTerminated()) {
            if (message instanceof SignalEnvelope signalEnvelope) {
                context.tell(signalEnvelope);
            }else {
                Envelope envelope=new Envelope(message,sender,this,MessageType.NORMAL,0);
                context.tell(envelope);
            }

        }
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (!isTerminated()) {
            SignalEnvelope envelope = createSignalEnvelope(signal, sender);
            tell(envelope);
        }
    }


    public void tell(SignalEnvelope envelope) {
        if (!isTerminated()) {
            context.tell(envelope);
        }
    }

    private SignalEnvelope createSignalEnvelope(Signal signal, ActorRef sender) {
        return SignalEnvelope.builder()
                .signal(signal)
                .sender(sender != null ? sender : ActorRef.noSender())
                .receiver((ActorRef<Signal>) getSelfRef())
                .priority(Envelope.Priority.HIGH)  // 信号消息优先级高
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


    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        return AskPattern.ask(
                this,
                timeout,
                replyTo -> message
        );
    }

    public <R> CompletableFuture<R> ask(T message) {
        return ask(message, Duration.ofSeconds(5)); // 默认5秒超时
    }

    public void setContext(ActorContext context) {
        if (this.context!=null){
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
    }
}
