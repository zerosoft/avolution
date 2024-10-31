package com.avolution.actor.impl;

import com.avolution.actor.core.*;
import com.avolution.actor.context.ActorContextImpl;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.message.DeadLetterOffice;
import com.avolution.actor.supervision.SupervisorStrategy;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;

public class ActorSystemImpl implements IActorSystem {
    private static final AtomicBoolean systemCreated = new AtomicBoolean(false);
    
    private final String name;
    private final Map<String, ActorRef> actors;
    private final Dispatcher dispatcher;
    private final ActorRef deadLetterOffice;
    private final ActorRef rootActor;
    private final AtomicBoolean terminated;
    private final CompletableFuture<Terminated> terminationFuture;
    private final SystemGuardian systemGuardian;

    public ActorSystemImpl(String name) {
        if (systemCreated.getAndSet(true)) {
            throw new IllegalStateException("Actor system already created");
        }
        
        this.name = name;
        this.actors = new ConcurrentHashMap<>();
        this.terminated = new AtomicBoolean(false);
        this.terminationFuture = new CompletableFuture<>();
        
        // 初始化调度器
        this.dispatcher = new Dispatcher(
            Runtime.getRuntime().availableProcessors(),
            1000  // 默认队列大小
        );

        try {
            // 创建系统守护者
            this.systemGuardian = new SystemGuardian();
            
            // 创建根Actor
            this.rootActor = createRootActor();
            
            // 创建死信办公室
            this.deadLetterOffice = createDeadLetterOffice();
            
            // 订阅死信
            DeadLetterActorRef.INSTANCE.subscribe(
                deadLetter -> deadLetterOffice.tell(deadLetter, ActorRef.noSender())
            );
            
        } catch (Exception e) {
            terminate();
            throw new ActorInitializationException("Failed to initialize actor system", e);
        }
    }

    @Override
    public ActorRef actorOf(Props props, String name) {
        validateActorName(name);
        checkTerminated();
        
        String path = buildActorPath(name);
        if (actors.containsKey(path)) {
            throw new InvalidActorNameException(
                "Actor with name '" + name + "' already exists"
            );
        }

        try {
            // 创建Actor实例
            Actor actor = props.newActor();
            
            // 创建上下文
            ActorContextImpl context = new ActorContextImpl(
                this,
                null, // 将在ActorRef创建时设置
                rootActor,
                actor,
                props.getSupervisorStrategy() != null 
                    ? props.getSupervisorStrategy() 
                    : DefaultSupervisorStrategy.INSTANCE,
                dispatcher
            );

            // 创建ActorRef
            ActorRefImpl ref = new ActorRefImpl(
                path,
                actor,
                context,
                dispatcher
            );
            
            // 注册Actor
            actors.put(path, ref);
            
            // 初始化Actor
            dispatcher.execute(() -> {
                try {
                    actor.preStart();
                } catch (Exception e) {
                    handleActorInitializationFailure(ref, e);
                }
            });

            return ref;
            
        } catch (Exception e) {
            throw new ActorInitializationException(
                "Failed to create actor: " + name, 
                e
            );
        }
    }

    @Override
    public void stop(ActorRef actor) {
        if (actor == null) return;
        
        ActorRefImpl actorRef = (ActorRefImpl) actors.remove(actor.path());
        if (actorRef != null) {
            dispatcher.execute(() -> {
                try {
                    actorRef.stop();
                } catch (Exception e) {
                    systemGuardian.handleError(
                        "Error stopping actor: " + actor.path(), 
                        e
                    );
                }
            });
        }
    }

    @Override
    public void terminate() {
        if (terminated.compareAndSet(false, true)) {
            try {
                // 停止所有actors
                stopAllActors();
                
                // 关闭调度器
                dispatcher.shutdown();
                
                // 清理资源
                cleanup();
                
                // 完成终止
                terminationFuture.complete(new Terminated(name));
                
            } catch (Exception e) {
                terminationFuture.completeExceptionally(e);
                throw new ActorSystemTerminationException(
                    "Failed to terminate actor system", 
                    e
                );
            } finally {
                systemCreated.set(false);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public com.avolution.actor.ActorRef actorOf(com.avolution.actor.Props props, String name) {
        return null;
    }

    public CompletionStage<Terminated> getWhenTerminated() {
        return terminationFuture;
    }

    public ActorRef deadLetters() {
        return DeadLetterActorRef.INSTANCE;
    }

    public ActorRef getDeadLetterOffice() {
        return deadLetterOffice;
    }

    // 内部辅助方法
    private ActorRef createRootActor() {
        Props rootProps = Props.create(RootActor.class)
            .withSupervisorStrategy(new RootSupervisorStrategy());
        
        return actorOf(rootProps, "root");
    }

    private ActorRef createDeadLetterOffice() {
        Props deadLetterOfficeProps = Props.create(
            () -> new DeadLetterOffice(1000)
        );
        return actorOf(deadLetterOfficeProps, "deadLetters");
    }

    private void stopAllActors() {
        // 创建actors的副本以避免并发修改
        var actorsCopy = new CopyOnWriteArrayList<>(actors.values());
        
        // 首先停止非系统actors
        actorsCopy.stream()
            .filter(ref -> !isSystemActor(ref))
            .forEach(this::stop);
            
        // 然后停止系统actors
        actorsCopy.stream()
            .filter(this::isSystemActor)
            .forEach(this::stop);
    }

    private boolean isSystemActor(ActorRef ref) {
        return ref.path().startsWith("system");
    }

    private void cleanup() {
        actors.clear();
        DeadLetterActorRef.INSTANCE.unsubscribeAll();
    }

    private void checkTerminated() {
        if (terminated.get()) {
            throw new IllegalStateException("Actor system is terminated");
        }
    }

    private void validateActorName(String name) {
        if (name == null || name.isEmpty()) {
            throw new InvalidActorNameException("Actor name must not be empty");
        }
        if (name.contains("/") || name.contains(":")) {
            throw new InvalidActorNameException(
                "Actor name must not contain '/' or ':'"
            );
        }
    }

    private String buildActorPath(String name) {
        return name.startsWith("/") ? name : "/" + name;
    }

    private void handleActorInitializationFailure(ActorRef ref, Exception e) {
        systemGuardian.handleError(
            "Actor initialization failed: " + ref.path(), 
            e
        );
        stop(ref);
    }

    // 内部类
    private class SystemGuardian {
        void handleError(String message, Throwable error) {
            // 这里可以添加日志、监控等
            System.err.println(message + ": " + error.getMessage());
        }
    }

    // 异常类
    public static class ActorInitializationException extends RuntimeException {
        public ActorInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ActorSystemTerminationException extends RuntimeException {
        public ActorSystemTerminationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidActorNameException extends RuntimeException {
        public InvalidActorNameException(String message) {
            super(message);
        }
    }

    public static record Terminated(String systemName) {}
} 