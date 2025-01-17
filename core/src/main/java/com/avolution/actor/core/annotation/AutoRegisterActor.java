package com.avolution.actor.core.annotation;

import com.avolution.actor.core.TypedActor;
import com.avolution.actor.message.Envelope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
/**
 * AutoRegisterActor 是一个抽象类，继承自 TypedActor<T>，用于自动注册消息处理器。
 * 该类通过扫描带有 @OnReceive 注解的方法，自动注册消息处理器，并根据消息类型调用相应的处理器。
 *
 * @param <T> 消息类型
 */
public abstract class AutoRegisterActor<T> extends TypedActor<T> {

    /**
     * 处理接收到的消息。
     * 该方法会根据消息的类型查找对应的处理器，并调用该处理器处理消息。
     * 如果未找到特定类型的处理器，则使用默认处理器。
     *
     * @param message 接收到的消息
     * @throws Exception 如果处理消息时发生异常
     */
    @Override
    protected void onReceive(T message) throws Exception {
        Envelope envelope = getCurrentEnvelope(); // 假设有一个方法可以获取当前的消息信封
        if (envelope == null) {
            logger.warn("No envelope available for message: {}", message);
            return;
        }

        Class<?> messageType = message.getClass();
        BiConsumer<Object, Envelope> handler = handlers.get(messageType);

        if (handler != null) {
            handler.accept(message, envelope);
        } else {
            // 如果没有找到特定类型的处理器，使用默认处理器
            BiConsumer<Object, Envelope> defaultHandler = handlers.get(Object.class);
            if (defaultHandler != null) {
                defaultHandler.accept(message, envelope);
            } else {
                logger.warn("No handler found for message type: {}", messageType);
            }
        }
    }
    /**
     * Actor 启动前的初始化方法。
     * 该方法会调用 registerHandlers() 方法注册消息处理器，并调用父类的 preStart() 方法。
     *
     * @return 返回 true 表示初始化成功
     */
    @Override
    public boolean preStart() {
        registerHandlers();
        return super.preStart();
    }

    /**
     * 消息处理器映射
     * Key: 消息类型
     * Value: 处理器函数 (消息, 信封)
     */
    private final Map<Class<?>, BiConsumer<Object, Envelope>> handlers = new HashMap<>();

    /**
     * 注册消息处理器。
     * 该方法会注册默认处理器，并扫描带有 @OnReceive 注解的方法，注册相应的处理器。
     */
    private void registerHandlers() {
        // 注册默认处理器
        handlers.put(Object.class, (msg, env) -> {
            try {
                onReceive((T) msg);
            } catch (Exception e) {
                handleError(e, env);
            }
        });

        // 扫描注解处理器
        for (Method method : this.getClass().getDeclaredMethods()) {
            OnReceive annotation = method.getAnnotation(OnReceive.class);
            if (annotation != null) {
                registerMethodHandler(method, annotation);
            }
        }
    }

    /**
     * 注册方法处理器。
     * 该方法会根据 @OnReceive 注解中的消息类型，注册相应的处理器。
     *
     * @param method 带有 @OnReceive 注解的方法
     * @param annotation @OnReceive 注解
     */
    private void registerMethodHandler(Method method, OnReceive annotation) {
        Class<?> messageType = annotation.value();
        method.setAccessible(true);

        // 检查方法参数
        if (method.getParameterCount() == 1 && messageType.isAssignableFrom(method.getParameterTypes()[0])) {
            // 单参数处理器
            handlers.put(messageType, (msg, env) -> invokeHandler(method, msg, env));
        } else if (method.getParameterCount() == 2 &&
                messageType.isAssignableFrom(method.getParameterTypes()[0]) &&
                Envelope.class.isAssignableFrom(method.getParameterTypes()[1])) {
            // 双参数处理器（消息和信封）
            handlers.put(messageType, (msg, env) -> invokeHandler(method, msg, env));
        } else {
            logger.warn("Invalid handler method signature: {}", method);
        }
    }

    /**
     * 调用消息处理器。
     * 该方法会根据方法的参数数量，调用相应的处理器方法。
     *
     * @param method 处理器方法
     * @param message 消息
     * @param envelope 信封
     */
    private void invokeHandler(Method method, Object message, Envelope envelope) {
        try {
            if (method.getParameterCount() == 1) {
                method.invoke(this, message);
            } else {
                method.invoke(this, message, envelope);
            }
        } catch (InvocationTargetException e) {
            handleError(e.getCause(), envelope);
        } catch (Exception e) {
            handleError(e, envelope);
        }
    }


}
