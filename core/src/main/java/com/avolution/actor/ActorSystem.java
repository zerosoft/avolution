package com.avolution.actor;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActorSystem {

    private final String name;

    private Actor rootActor;

    private final Map<String, ActorRef> actors = new ConcurrentHashMap<>();

    private final AtomicBoolean isTerminated = new AtomicBoolean(false);

    private final CompletableFuture<Void> terminationFuture = new CompletableFuture<>();

    private ActorSystem(String name) {
        this.name = name;
        rootActor=new RootActor(this);
    }

    public static ActorSystem create(String name) {
        return new ActorSystem(name);
    }

    public ActorRef actorOf(Props props, String name) {
        if (isTerminated.get()) {
            throw new IllegalStateException("Actor system is terminated");
        }

        String path = "usr/" + name;
        if (actors.containsKey(path)) {
            throw new IllegalArgumentException("Actor with path " + path + " already exists");
        }

        try {
            Actor actor = props.newActor();
            ActorRef ref = new ActorRefImpl(path, actor, new ActorContextImpl(this, null, null));
            actor.setContext(new ActorContextImpl(this, ref, null), ref);
            actor.preStart();

            actors.put(path, ref);
            return ref;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create actor", e);
        }
    }

    public void stop(ActorRef actor) {
        ActorRefImpl actorRef = (ActorRefImpl) actors.remove(actor.path());
        if (actorRef != null) {
            actorRef.stop();
        }
    }

    public void terminate() {
        if (isTerminated.compareAndSet(false, true)) {
            actors.values().forEach(this::stop);
            terminationFuture.complete(null);
        }
    }

    public CompletionStage<Void> getWhenTerminated() {
        return terminationFuture;
    }

    public void processMessage(Object message) {

    }
}