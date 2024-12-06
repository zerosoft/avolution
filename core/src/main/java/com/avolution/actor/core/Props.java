package com.avolution.actor.core;


import java.lang.reflect.Constructor;
import java.util.function.Supplier;
import com.avolution.actor.exception.ActorCreationException;

public class Props<T> {

    private final Supplier<TypedActor<T>> factory;
    private final int throughput;
    
    private Props(Supplier<TypedActor<T>> factory, int throughput) {
        this.factory = factory;
        this.throughput = throughput;
    }

    /**
     * 创建Actor
     * @param actorClass
     * @return
     * @param <T>
     */
    public static <T> Props<T> create(Class<? extends TypedActor<T>> actorClass) {
        return new Props<>(() -> {
            try {
                Constructor<? extends TypedActor<T>> declaredConstructor = actorClass.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                return declaredConstructor.newInstance();
            } catch (Exception e) {
                throw new ActorCreationException("Failed to create actor instance", e);
            }
        }, 100);
    }

    /**
     * 创建带参数的Actor
     * @param actorClass
     * @param params
     * @return
     * @param <T>
     */
    public static <T> Props<T> create(Class<? extends TypedActor<T>> actorClass, Object... params) {
        return new Props<>(() -> {
            try {
                Class<?>[] paramTypes = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    paramTypes[i] = params[i].getClass();
                }
                Constructor<? extends TypedActor<T>> constructor = actorClass.getDeclaredConstructor(paramTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(params);
            } catch (Exception e) {
                throw new ActorCreationException("Failed to create actor instance with parameters", e);
            }
        }, 100);
    }

    public static <T> Props<T> create(Supplier<TypedActor<T>> factory) {
        return new Props<>(factory, 100);
    }


    public TypedActor<T> newActor() {
        return factory.get();
    }


    public int throughput() {
        return throughput;
    }
}