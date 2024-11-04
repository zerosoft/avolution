package com.avolution.actor.core;

import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;
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

    public static <T> Props<T> create(Class<? extends AbstractActor<T>> actorClass) {
        return new Props<>(() -> {
            try {
                return actorClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new ActorCreationException("Failed to create actor instance", e);
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