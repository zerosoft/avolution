package com.avolution.actor.core;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import com.avolution.actor.core.context.ActorContextManager;
import com.avolution.actor.core.context.ActorRefRegistry;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorSystemCreationException;
import com.avolution.actor.exception.SystemFailureException;
import com.avolution.actor.lifecycle.LifecycleState;
import com.avolution.actor.message.Signal;
import com.avolution.actor.message.SignalEnvelope;
import com.avolution.actor.message.SignalPriority;
import com.avolution.actor.message.SignalScope;
import com.avolution.actor.supervision.DeathWatch;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.system.actor.*;
import com.avolution.actor.exception.ActorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Actor系统的核心实现类，负责管理整个Actor生态系统
 *
 * 主要功能：
 * 1. Actor生命周期管理
 *    - 创建和停止Actor
 *    - 维护Actor层级关系
 *    - 管理Actor状态转换
 *
 * 2. 消息分发
 *    - 提供消息调度器
 *    - 处理死信消息
 *    - 支持定时消息
 *
 * 3. 监督管理
 *    - 实现监督策略
 *    - 处理Actor故障
 *    - 提供死亡监控
 *
 * 4. 系统服务
 *    - 死信Actor服务
 *    - 系统守护Actor
 *    - 调度服务
 *
 * 核心组件：
 * @field name          - 系统唯一标识名
 * @field actors        - 管理所有Actor引用
 * @field contexts      - 管理Actor上下文
 * @field dispatcher    - 消息调度器
 * @field deathWatch   - 死亡监控服务
 * @field scheduler    - 调度服务
 * @field state        - 系统状态
 * @field deadLetters  - 死信处理Actor
 *
 * 使用示例：
 * <pre>
 * ActorSystem system = ActorSystem.create("my-system");
 * ActorRef<MyMessage> actor = system.actorOf(Props.create(MyActor.class), "myActor");
 * actor.tell(new MyMessage(), ActorRef.noSender());
 * system.terminate();
 * </pre>
 *
 * @see ActorRef
 * @see ActorContext
 * @see Dispatcher
 * @see DeathWatch
 */
public class ActorSystem {
    private static final Logger log = LoggerFactory.getLogger(ActorSystem.class);

    private static final AtomicReference<ActorSystem> INSTANCE = new AtomicReference<>();
    private static final ConcurrentHashMap<String, ActorSystem> NAMED_SYSTEMS = new ConcurrentHashMap<>();
    // 系统名称
    private final String name;
    // 系统组件
    private final ActorRefRegistry refRegistry;
    private final ActorContextManager contextManager;
    // 系统服务
    private final Dispatcher dispatcher;
    private final DeathWatch deathWatch;
    // 系统状态
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<SystemState> state;
    private final CompletableFuture<Void> terminationFuture;

    // 系统Actor
    private  ActorRef<IDeadLetterActorMessage> deadLetters;
    private  ActorRef<SystemGuardianActorMessage> systemGuardian;
    private  ActorRef<UserGuardianActorMessage> userGuardian;

    /**
     * 创建或获取默认的ActorSystem实例
     * @return 默认的ActorSystem实例
     */
    public static ActorSystem create() {
        return create("default");
    }

    /**
     * 创建或获取指定名称的ActorSystem实例
     * @param name 系统名称
     * @return ActorSystem实例
     * @throws IllegalStateException 如果尝试创建同名的系统
     */
    public static synchronized ActorSystem create(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("System name cannot be null or empty");
        }

        return NAMED_SYSTEMS.computeIfAbsent(name, key -> {
            if (INSTANCE.get() == null) {
                ActorSystem system = new ActorSystem(key);
                if (INSTANCE.compareAndSet(null, system)) {
                    return system;
                }
            }
            throw new IllegalStateException(
                    "Another ActorSystem is already active. Only one ActorSystem can exist in a JVM.");
        });
    }


    private ActorSystem(String name) {
        this.name = name;
        this.dispatcher = new Dispatcher();
        this.deathWatch = new DeathWatch(this);
        this.scheduler = new VirtualThreadScheduler();
        this.state = new AtomicReference<>(SystemState.NEW);
        this.terminationFuture = new CompletableFuture<>();
        this.contextManager = new ActorContextManager();
        this.refRegistry=new ActorRefRegistry(this);
        start();
    }

    private void start() {
        if (state.compareAndSet(SystemState.NEW, SystemState.RUNNING)) {
            log.info("Actor system '{}' started", name);
        }
        // 创建系统Actor
        try {
            initializeSystemActors();
        } catch (ActorSystemCreationException e) {
            throw new RuntimeException(e);
        }
    }


    private void initializeSystemActors() throws ActorSystemCreationException {
        try {
            // 1. 创建死信Actor - 最基础的系统服务
            this.deadLetters = createAndVerifySystemActor(
                    DeadLetterActor.class,
                    "/system/deadLetters",
                    "DeadLetter Actor"
            );

            // 2. 创建系统守护Actor - 管理系统级Actor
            this.systemGuardian = createAndVerifySystemActor(
                    SystemGuardianActor.class,
                    "/system/guardian",
                    "System Guardian"
            );

            // 3. 创建用户守护Actor - 管理用户级Actor
            this.userGuardian = createAndVerifySystemActor(
                    UserGuardianActor.class,
                    "/system/user",
                    "User Guardian"
            );

            log.info("System actors initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize system actors", e);
            throw new ActorSystemCreationException("System actors initialization failed", e);
        }
    }

    private <T> ActorRef<T> createAndVerifySystemActor(
            Class<? extends AbstractActor<T>> actorClass,
            String path,
            String actorName) throws ActorSystemCreationException {

        ActorRef<T> ref = createSystemActor(actorClass, path);
        if (ref == null) {
            throw new ActorSystemCreationException(
                    String.format("Failed to create %s at path: %s", actorName, path)
            );
        }

        // 等待Actor完全初始化
        try {
            waitForActorInitialization(ref);
        } catch (Exception e) {
            throw new ActorSystemCreationException(
                    String.format("Failed to initialize %s", actorName), e
            );
        }

        return ref;
    }

    private <T> ActorRef<T> createSystemActor(Class<? extends AbstractActor<T>> actorClass,String path) throws ActorSystemCreationException {
        try {
            Props<T> props = Props.create(actorClass);
            AbstractActor<T> actor = props.newActor();
            ActorContext context = new ActorContext(path, this, actor, null, props);
            String[] split = path.split("/");
            LocalActorRef<T> actorRef = new LocalActorRef<>(actor,path, split[split.length-1],null);
            // 设置上下文
            actor.setContext(context);
            // 设置自身引用
            actor.setSelfRef(actorRef);
            // 初始化Actor
            context.initializeActor();

            // 注册系统Actor
            registerSystemActor(actorRef, context);

            log.debug("Created system actor: {}", path);
            return actorRef;
        } catch (Exception e) {
            log.error("Failed to create system actor at path: {}", path, e);
            throw new ActorSystemCreationException("Failed to create system actor", e);
        }
    }


    private String generateActorPath(String name, ActorContext parentContext) {
        String basePath = (parentContext != null) ?
                parentContext.getPath() : "/user";
        return basePath + "/" + name + "#" + generateUniqueId(name);
    }

    private void registerSystemActor(ActorRef<?> ref, ActorContext context) {
//        refRegistry.registerSystem(ref);
        contextManager.addContext(ref.path(), context);
    }

    private void registerUserActor(ActorRef<?> ref, ActorContext context) {
//        refRegistry.registerUser(ref, context.getParent() == null ? "/user" : context.getParent().getPath());
        contextManager.addContext(ref.path(), context);
    }


    private void waitForActorInitialization(ActorRef<?> ref) {
        // 等待Actor进入RUNNING状态
        long deadline = System.currentTimeMillis() + 5000; // 5秒超时
        while (System.currentTimeMillis() < deadline) {
            if (contextManager.getContext(ref.path()).get().getState().get() == LifecycleState.RUNNING) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ActorInitializationException("Actor initialization interrupted");
            }
        }
        throw new ActorInitializationException("Actor initialization timeout");
    }

    public <T> ActorRef<T> actorOf(Props<T> props, String name) {
        return actorOf(props, name, null);
    }


    private final Map<String,AtomicInteger> pathId = new HashMap<>();

    public int generateUniqueId(String path) {
        AtomicInteger atomicInteger = pathId.computeIfAbsent(path, k -> new AtomicInteger(0));
        return atomicInteger.incrementAndGet();
    }

    public <T> ActorRef<T> actorOf(Props<T> props, String name, ActorContext actorContextRef) {
        // 验证系统状态和Actor名称
        if (state.get() != SystemState.RUNNING) {
            throw new IllegalStateException("Actor system is not running");
        }

        validateActorName(name);

        String path;
        if (actorContextRef != null) {
            path = actorContextRef.getPath() + "/" + name;
        } else {
            path = "/user/" + name;
        }

        // 检查是否已存在
        if (contextManager.hasContext(path)) {
            throw new ActorCreationException("Actor already exists at path: " + path);
        }

        try {
            AbstractActor<T> actor = props.newActor();
            ActorContext context = new ActorContext(path, this, actor, actorContextRef, props);

            LocalActorRef<T> actorRef = new LocalActorRef<>(actor,path,name,deadLetters);
            actor.setContext(context);
            actor.setSelfRef(actorRef);

            context.start();
            if (actorContextRef!=null){
                actorContextRef.addChild(name,actor);
            }
            // 注册Actor
            registerActor(actorRef, context);

            log.debug("Created actor: {}", path);
            return actorRef;
        } catch (Exception e) {
            log.error("Failed to create actor: {}", name, e);
            throw new ActorCreationException("Failed to create actor: " + name, e);
        }
    }


    private void validateActorName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Actor name cannot be null or empty");
        }

        // 检查名称长度
        if (name.length() > 256) {
            throw new IllegalArgumentException("Actor name is too long (max 256 characters)");
        }

        // 检查名称格式
        if (!name.matches("^[a-zA-Z0-9_\\-]+$")) {
            throw new IllegalArgumentException(
                    "Actor name can only contain alphanumeric characters, underscores and hyphens"
            );
        }

        // 检查保留字
        String[] reservedNames = {"system", "user", "temp", "deadLetters", "guardian"};
        for (String reserved : reservedNames) {
            if (name.equalsIgnoreCase(reserved)) {
                throw new IllegalArgumentException("Actor name '" + name + "' is reserved");
            }
        }

        // 检查特殊字符
        if (name.contains("/") || name.contains("#")) {
            throw new IllegalArgumentException(
                    "Actor name cannot contain path separators (/) or hash (#) characters"
            );
        }
    }

    public CompletableFuture<Void> stop(ActorRef<?> actor) {
        if (actor == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (state.get() != SystemState.RUNNING) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Actor system is not running")
            );
        }

        CompletableFuture<Void> stopFuture = new CompletableFuture<>();
        try {
            // 创建高优先级的停止信号
            // 发送停止信号
            actor.tell(Signal.KILL,ActorRef.noSender());

            // 设置超时处理
            return stopFuture.orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        if (throwable instanceof TimeoutException) {
                            log.warn("Timeout while stopping actor: {}, sending KILL signal", actor.path());
                            // 发送 KILL 信号强制停止
                            actor.tell(Signal.KILL,ActorRef.noSender());
                        } else {
                            log.error("Error while stopping actor: {}", actor.path(), throwable);
                        }
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to initiate actor stop: {}", actor.path(), e);
            stopFuture.completeExceptionally(e);
            return stopFuture;
        }
    }

    public CompletableFuture<Void> terminate() {
        if (state.compareAndSet(SystemState.RUNNING, SystemState.TERMINATING)) {
            try {
                log.info("Terminating actor system '{}'...", name);

                // 1. 停止用户Actor
                stopUserActors();

                // 2. 停止系统Actor
                stopSystemActors();

                // 3. 关闭内部组件
                shutdownInternals();

                // 4. 设置终止状态
                state.set(SystemState.TERMINATED);
                terminationFuture.complete(null);

                // 清理系统实例
                NAMED_SYSTEMS.remove(this.name);
                if (INSTANCE.get() == this) {
                    INSTANCE.set(null);
                }

                state.set(SystemState.TERMINATED);
                terminationFuture.complete(null);

                log.info("Actor system '{}' terminated", name);
            } catch (Exception e) {
                log.error("Error during actor system termination", e);
                terminationFuture.completeExceptionally(e);
            }
        }
        return terminationFuture;
    }

    public void handleSystemFailure(Throwable cause, ActorRef<?> failedActor) {
        try {
            log.error("System level failure from actor: {}", failedActor.path(), cause);

            // 2. 通知死亡监视器
            deathWatch.signalTermination(failedActor, false);

            // 3. 发送到系统守护者
            systemGuardian.tell(Signal.SYSTEM_FAILURE, ActorRef.noSender());

            // 4. 根据错误严重程度决定是否需要停止 Actor
            if (isSystemFailure(cause)) {
                stopActor(failedActor)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("Failed to stop actor after system failure: {}", failedActor.path(), ex);
                            return null;
                        });
            }
        } catch (Exception e) {
            log.error("Error handling system failure", e);
        }
    }

    private CompletableFuture<Void> stopActor(ActorRef<?> actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();
        try {
            // 1. 获取Actor上下文
            Optional<ActorContext> contextOptional = contextManager.getContext(actor.path());
            if (contextOptional.isEmpty()) {
                stopFuture.completeExceptionally(
                        new IllegalStateException("Actor context not found: " + actor.path()));
                return stopFuture;
            }

            ActorContext context=contextOptional.get();
            // 2. 停止子Actor
            CompletableFuture<Void> childrenStopFuture = stopChildren(context);

            // 3. 停止当前Actor
            childrenStopFuture.thenRun(() -> {
                try {
                    // 执行停止流程
                    context.stop()
                            .thenRun(() -> {
                                // 清理注册信息
                                unregisterActor(actor.path());
                                // 通知死亡监视器
                                deathWatch.signalTermination(actor, true);
                                stopFuture.complete(null);
                            })
                            .exceptionally(ex -> {
                                stopFuture.completeExceptionally(ex);
                                return null;
                            });
                } catch (Exception e) {
                    stopFuture.completeExceptionally(e);
                }
            });

        } catch (Exception e) {
            stopFuture.completeExceptionally(e);
        }
        return stopFuture;
    }

    private CompletableFuture<Void> stopChildren(ActorContext context) {
        List<CompletableFuture<Void>> childrenFutures = new ArrayList<>();

        // 获取所有子Actor的引用
        Map<String, ActorRef> children = context.getChildren();

        // 停止每个子Actor
        for (ActorRef child : children.values()) {
            childrenFutures.add(stopActor(child));
        }

        // 等待所有子Actor停止完成
        return CompletableFuture.allOf(
                childrenFutures.toArray(new CompletableFuture[0])
        );
    }

    private boolean isSystemFailure(Throwable cause) {
        return cause instanceof SystemFailureException
                || cause instanceof OutOfMemoryError
                || cause instanceof StackOverflowError;
    }

    private void stopUserActors() {
        // 最后停止用户守护者
        if (userGuardian != null) {
            stop(userGuardian);
        }
    }

    private void stopSystemActors() {
        // 按照依赖顺序反向停止系统Actor
        if (systemGuardian != null) {
            stop(systemGuardian);
        }
        if (deadLetters != null) {
            stop(deadLetters);
        }
    }

    private void shutdownInternals() {
        try {
            // 关闭调度器
            if (scheduler != null) {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            }

            // 关闭消息分发器
            if (dispatcher != null) {
                dispatcher.shutdown();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while shutting down internals", e);
        }
    }

    public CompletableFuture<Void> getWhenTerminated() {
        return terminationFuture;
    }

    public void registerActor(ActorRef<?> ref, ActorContext context) {
        refRegistry.register(ref,context.getParent()==null?"/usr":context.getParent().getPath());
        contextManager.addContext(ref.path(), context);
    }

    public void unregisterActor(String path) {
        refRegistry.unregister(path,"");
        contextManager.removeContext(path);
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

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public ActorRef<IDeadLetterActorMessage> getDeadLetters() {
        return deadLetters;
    }

    public ActorContextManager getContextManager() {
        return contextManager;
    }

    public ActorRefRegistry getRefRegistry() {
        return refRegistry;
    }
}
