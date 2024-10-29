package com.avolution.actor;

import java.util.Arrays;

public class Props {

    private final Class<? extends Actor> actorClass;

    private final Object[] args;

    private final String dispatcher;

    private Props(Class<? extends Actor> actorClass, Object[] args, String dispatcher) {
        this.actorClass = actorClass;
        this.args = args;
        this.dispatcher = dispatcher;
    }

    public static Props create(Class<? extends Actor> actorClass, Object... args) {
        return new Props(actorClass, args, "default");
    }

    public Props withDispatcher(String dispatcher) {
        return new Props(this.actorClass, this.args, dispatcher);
    }

    Actor newActor() throws Exception {
        if (args.length == 0) {
            return actorClass.getDeclaredConstructor().newInstance();
        }
        // Simple constructor matching for demonstration
        return actorClass.getDeclaredConstructor(Arrays.stream(args)
                        .map(Object::getClass)
                        .toArray(Class<?>[]::new))
                .newInstance(args);
    }
}
