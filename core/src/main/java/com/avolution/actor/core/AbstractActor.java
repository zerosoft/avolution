package com.avolution.actor.core;


import com.avolution.actor.context.ActorContext;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.exception.AskTimeoutException;
import com.avolution.actor.lifecycle.LifecycleState;
import  com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageHandler;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
    // 持有唯一的ActorRefProxy引用
    private ActorRefProxy<T> selfRef;
    /**
     * 消息处理器
     */
    private final Map<Class<?>, Consumer<Object>> handlers = new HashMap<>();

    public AbstractActor() {
        registerHandlers();
    }

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
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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
        return currentMessage.getSender();
    }


    protected void setSelfRef(ActorRefProxy<T> ref) {
        if (this.selfRef == null) {
            this.selfRef = ref;
        }
    }

    public ActorRef<T> getSelf() {
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
        if (!isTerminated()) {
            Envelope<T> envelope=new Envelope<>(message,sender,this,MessageType.NORMAL,1);
            context.tell(envelope);
        }
    }

    @Override
    public void tell(Signal message, ActorRef sender) {
        if (!isTerminated()) {
            Envelope envelope=new Envelope(message,sender,this,MessageType.SYSTEM,1);
            context.tell(envelope);
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

    // Actor正式初始换之前的
    public void preStart() {

    }
    // 生命周期回调方法
    public void postStop() {
        destroy();
    }

    public void preRestart(Throwable reason) {
        // 在重启之前的处理逻辑
    }

    public void postRestart(Throwable reason) {
        // 在重启之后的处理逻辑
    }

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

            // 处理系统信号
            if (message.getMessage() instanceof Signal signal) {
                signal.handle(this);
                return;
            }

            onReceive(currentMessage.getMessage());

            long processingTime = System.nanoTime() - startTime;

        } catch (Exception e) {
            context.handleFailure(e, message);
        } finally {
            currentMessage = null;
        }
    }

    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        CompletableFuture<R> future = new CompletableFuture<>();

        // 创建临时Actor接收响应
        Props<R> tempProps = Props.create(() -> new AbstractActor<R>() {
            @Override
            public void onReceive(R message) {
                future.complete(message);
                context.stop();
            }
        });

        // 创建临时Actor
        ActorRef<R> tempActor = context.actorOf(tempProps, "temp-" + UUID.randomUUID());

        // 发送消息
        tell(message, tempActor);

        // 设置超时
        context.system().scheduler().schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(
                        new AskTimeoutException("Ask timed out after " + timeout)
                );
                context.stop(tempActor);
            }
        }, 5L, TimeUnit.SECONDS);

        return future;
    }

    public <R> CompletableFuture<R> ask(T message) {
        return ask(message, Duration.ofSeconds(5)); // 默认5秒超时
    }

    public void destroy() {
        if (selfRef != null) {
            selfRef.invalidate();
            selfRef = null;
        }
        context = null;
        currentMessage = null;
        lifecycleState = LifecycleState.STOPPED;
    }
}
