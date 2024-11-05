package com.avolution.actor.core;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.supervision.DeathWatch;
import com.avolution.actor.context.ActorContext;
import com.avolution.actor.system.actor.*;
import com.avolution.actor.exception.ActorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ActorSystem {
    private static final Logger log = LoggerFactory.getLogger(ActorSystem.class);

    private final String name;

    private final Map<String, ActorRef<?>> actors;

    private final Map<String, ActorContext> contexts;

    private final Dispatcher dispatcher;
    private final DeathWatch deathWatch;

    private final ScheduledExecutorService scheduler;
    private final AtomicReference<SystemState> state;
    private final CompletableFuture<Void> terminationFuture;

    // 系统Actor
    private  ActorRef<IDeadLetterActorMessage> deadLetters;
    private  ActorRef systemGuardian;
    private  ActorRef userGuardian;

    public ActorSystem(String name) {
        this.name = name;
        this.actors = new ConcurrentHashMap<>();
        this.contexts = new ConcurrentHashMap<>();
        this.dispatcher = new Dispatcher();
        this.deathWatch = new DeathWatch(this);
        this.scheduler = new VirtualThreadScheduler();
        this.state = new AtomicReference<>(SystemState.NEW);
        this.terminationFuture = new CompletableFuture<>();

//        this.systemGuardian = createSystemActor(SystemGuardian.class, "/system/guardian");
//        this.userGuardian = createSystemActor(UserGuardian.class, "/user");

        start();
    }

    private ActorRef createSystemActor(Class systemClass, String path) {
        ActorRef iDeadLetterActorMessageActorRef = actorOf(Props.create(systemClass), path);
        return iDeadLetterActorMessageActorRef;
    }

    private void start() {
        if (state.compareAndSet(SystemState.NEW, SystemState.RUNNING)) {
            log.info("Actor system '{}' started", name);
        }
        // 创建系统Actor
        this.deadLetters = createSystemActor(DeadLetterActor.class, "/system/deadLetters");
    }

    public <T> ActorRef<T> actorOf(Props<T> props, String name) {
        return actorOf(props, name, null);
    }

    public <T> ActorRef<T> actorOf(Props<T> props, String name,ActorRef actorContextRef) {
        // 验证系统状态和Actor名称
        if (state.get() != SystemState.RUNNING) {
            throw new IllegalStateException("Actor system is not running");
        }
//        validateActorName(name);

        String path = "/user/" + name;

        // 检查是否已存在
        if (actors.containsKey(path)) {
            throw new ActorCreationException("Actor '" + name + "' already exists");
        }

        try {
            // 创建Actor实例
            AbstractActor<T> actor = props.newActor();

            // 创建ActorContext
            ActorContext context = new ActorContext(path,
                    this,
                    actor,
                    actorContextRef!=null?actorContextRef:systemGuardian,
                    props
            );

            // 注册Actor
            actors.put(path, actor);
            contexts.put(path, context);

            // 初始化Actor
            actor.initialize(context);

            log.debug("Created actor: {}", path);
            return actor;

        } catch (Exception e) {
            log.error("Failed to create actor: {}", name, e);
            throw new ActorCreationException("Failed to create actor: " + name, e);
        }
    }

    public  ActorContext getContext(String path) {
        return contexts.get(path);
    }

    public void stop(ActorRef actor) {
        String path = actor.path();
        actors.remove(path);
        ActorContext context = contexts.remove(path);
        if (context != null) {
            context.stop();
//            context.getChildren().values().forEach(this::stop);
        }
//        actor.tell(PoisonPill.INSTANCE, ActorRef.noSender());
    }

    public CompletableFuture<Void> terminate() {
//        if (state.compareAndSet(SystemState.RUNNING, SystemState.TERMINATING)) {
//            stopUserActors();
//            stopSystemActors();
//            shutdownInternals();
//            state.set(SystemState.TERMINATED);
//            terminationFuture.complete(null);
//        }
        return terminationFuture;
    }

    // Getters
    public String name() {
        return name;
    }

    public Dispatcher dispatcher() {
        return dispatcher;
    }

    public DeathWatch deathWatch() {
        return deathWatch;
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }
}