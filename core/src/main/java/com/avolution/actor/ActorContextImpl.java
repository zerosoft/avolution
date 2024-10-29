package com.avolution.actor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ActorContextImpl implements ActorContext {
    private final Map<String, ActorRef> children = new ConcurrentHashMap<>();
    private final ActorSystem system;
    private final ActorRef self;
    private final ActorRef parent;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private ActorRef sender;

    public ActorContextImpl(ActorSystem system, ActorRef self, ActorRef parent) {
        this.system = system;
        this.self = self;
        this.parent = parent;
    }

    @Override
    public ActorRef actorOf(Props props, String name) {
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Child with name " + name + " already exists");
        }

        ActorRef child = system.actorOf(props, self.path() + "/" + name);
        children.put(name, child);
        return child;
    }

    @Override
    public void stop(ActorRef actor) {
        system.stop(actor);
        children.values().remove(actor);
    }

    @Override
    public ActorRef self() { return self; }

    @Override
    public ActorRef sender() { return sender; }

    @Override
    public ActorRef parent() { return parent; }

    void setSender(ActorRef sender) {
        this.sender = sender;
    }
}
