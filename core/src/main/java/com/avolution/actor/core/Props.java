package com.avolution.actor.core;

import com.avolution.actor.supervision.SupervisorStrategy;
import java.util.function.Supplier;

public class Props {
    private final Supplier<Actor> factory;
    private SupervisorStrategy supervisorStrategy;
    
    private Props(Supplier<Actor> factory) {
        this.factory = factory;
    }
    
    public static Props create(Supplier<Actor> factory) {
        return new Props(factory);
    }
    
    public static Props create(Class<? extends Actor> actorClass) {
        return new Props(() -> {
            try {
                return actorClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create actor", e);
            }
        });
    }
    
    public Props withSupervisorStrategy(SupervisorStrategy strategy) {
        this.supervisorStrategy = strategy;
        return this;
    }
    
    public Actor newActor() {
        return factory.get();
    }
    
    public SupervisorStrategy getSupervisorStrategy() {
        return supervisorStrategy;
    }
} 