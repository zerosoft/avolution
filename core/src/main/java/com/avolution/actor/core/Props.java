package com.avolution.actor.core;

import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;
import com.avolution.actor.exception.ActorCreationException;

public class Props<T> {

    private final Supplier<AbstractActor<T>> factory;
    private final SupervisorStrategy supervisorStrategy;
    private final int throughput;
    
    private Props(Supplier<AbstractActor<T>> factory, 
                 SupervisorStrategy supervisorStrategy,
                 int throughput) {
        this.factory = factory;
        this.supervisorStrategy = supervisorStrategy;
        this.throughput = throughput;
    }

    /**
     * 创建Actor
     * @param actorClass
     * @return
     * @param <T>
     */
    public static <T> Props<T> create(Class<? extends AbstractActor<T>> actorClass) {
        return new Props<>(() -> {
            try {
                Constructor<? extends AbstractActor<T>> declaredConstructor = actorClass.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                return declaredConstructor.newInstance();
            } catch (Exception e) {
                throw new ActorCreationException("Failed to create actor instance", e);
            }
        }, DefaultSupervisorStrategy.INSTANCE, 100);
    }

    /**
     * 创建带参数的Actor
     * @param actorClass
     * @param params
     * @return
     * @param <T>
     */
    public static <T> Props<T> create(Class<? extends AbstractActor<T>> actorClass, Object... params) {
        return new Props<>(() -> {
            try {
                Class<?>[] paramTypes = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    paramTypes[i] = params[i].getClass();
                }
                Constructor<? extends AbstractActor<T>> constructor = actorClass.getDeclaredConstructor(paramTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(params);
            } catch (Exception e) {
                throw new ActorCreationException("Failed to create actor instance with parameters", e);
            }
        }, DefaultSupervisorStrategy.INSTANCE, 100);
    }

    public static <T> Props<T> create(Supplier<AbstractActor<T>> factory) {
        return new Props<>(factory, DefaultSupervisorStrategy.INSTANCE, 100);
    }

    public Props<T> withSupervisorStrategy(SupervisorStrategy strategy) {
        return new Props<>(this.factory, strategy, this.throughput);
    }

    public Props<T> withThroughput(int throughput) {
        return new Props<>(this.factory, this.supervisorStrategy, throughput);
    }

    public AbstractActor<T> newActor() {
        return factory.get();
    }

    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    public int throughput() {
        return throughput;
    }
}