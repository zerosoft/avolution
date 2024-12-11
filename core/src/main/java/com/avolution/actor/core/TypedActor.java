package com.avolution.actor.core;

import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.core.lifecycle.ActorLifecycleHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 *
 * @param <T>
 */
public abstract class TypedActor<T> implements ActorLifecycleHook {

    Logger logger= LoggerFactory.getLogger(TypedActor.class);

    /**
     * 消息处理器
     */
    private final Map<Class<?>, Consumer<Object>> handlers = new HashMap<>();

    private ActorContext actorContext;

    /**
     * 注册消息处理器
     */
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

    /**
     * 调用消息处理器
     * @param method 方法
     * @param message 消息
     */
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

    @Override
    public void preStart() {
        ActorLifecycleHook.super.preStart();
        registerHandlers();
    }

    public void setActorContext(ActorContext actorContext) {
        if (this.actorContext!=null){
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.actorContext = actorContext;
    }

    public ActorContext getContext() {
        return actorContext;
    }

    public ActorRef getSelf() {
        return getContext().getUnTypedActor().getSelfRef();
    }

    public ActorRef getSender() {
        return getContext().getUnTypedActor().getSender();
    }

    public String getPath() {
        return getContext().getPath();
    }

    protected abstract void onReceive(T message) throws Exception;

}
