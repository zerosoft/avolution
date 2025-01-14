package com.avolution.actor.core.annotation;

import com.avolution.actor.core.TypedActor;
import com.avolution.actor.message.Envelope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class AutoRegisterActor<T> extends TypedActor<T> {

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
     * 注册消息处理器
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
     * 注册方法处理器
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
     * 调用消息处理器
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

