package com.avolution.actor.core;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import com.avolution.actor.core.context.ActorContextManager;
import com.avolution.actor.core.context.ActorRefRegistry;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorSystemCreationException;
import com.avolution.actor.exception.SystemFailureException;
import com.avolution.actor.core.lifecycle.LifecycleState;
import com.avolution.actor.message.*;
import com.avolution.actor.stream.EventStream;
import com.avolution.actor.supervision.DeathWatch;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.system.actor.*;
import com.avolution.actor.exception.ActorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
    private static final Logger logger = LoggerFactory.getLogger(ActorSystem.class);

    private static final AtomicReference<ActorSystem> INSTANCE = new AtomicReference<>();
    private static final ConcurrentHashMap<String, ActorSystem> NAMED_SYSTEMS = new ConcurrentHashMap<>();
    // 系统名称
    private final String name;
    // 系统组件
    private final ActorRefRegistry refRegistry;
    private final ActorContextManager contextManager;
    private final EventStream eventStream;

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
        this.eventStream = new EventStream(this);
        start();
    }

    private void start() {
        if (state.compareAndSet(SystemState.NEW, SystemState.RUNNING)) {
            logger.info("Actor system '{}' started", name);
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
                    "/user",
                    "User Guardian"
            );

            logger.info("System actors initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize system actors", e);
            throw new ActorSystemCreationException("System actors initialization failed", e);
        }
    }

    private <T> ActorRef<T> createAndVerifySystemActor(
            Class<? extends TypedActor<T>> actorClass,
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

    private <T> ActorRef<T> createSystemActor(Class<? extends TypedActor<T>> actorClass, String path) throws ActorSystemCreationException {
        try {
            // 创建Props
            Props<T> props = Props.create(actorClass,this);
            TypedActor<T> typedActor = props.newActor();
            // 创建一个未类型化的Actor
            UnTypedActor<T> unTypedActor=new UnTypedActor<>(typedActor);
            // 创建上下文
            ActorContext context = new ActorContext(path, this, unTypedActor, null, props);
            // 设置上下文
            unTypedActor.setContext(context);
            // 设置自身引用
            String[] split = path.split("/");
            // 创建ActorRef
            LocalActorRef<T> actorRef = new LocalActorRef<>(unTypedActor,path, split[split.length-1],null);

            context.start();

            // 注册系统Actor
            registerSystemActor(actorRef, context);

            logger.debug("Created system typedActor: {}", path);
            return actorRef;
        } catch (Exception e) {
            logger.error("Failed to create system actor at path: {}", path, e);
            throw new ActorSystemCreationException("Failed to create system actor", e);
        }
    }


    private void registerSystemActor(ActorRef<?> ref, ActorContext context) {
        // 同时注册到RefRegistry和ContextManager
        refRegistry.register(ref, "/system");
        contextManager.addContext(ref.path(), context);
        logger.debug("Registered system actor: {}", ref.path());
    }

    private void registerUserActor(ActorRef<?> ref, ActorContext context) {
        String parentPath = context.getParent() == null ? "/user" : context.getParent().getPath();
        refRegistry.register(ref, parentPath);
        contextManager.addContext(ref.path(), context);
        logger.debug("Registered user actor: {}", ref.path());
    }

    /**
     * 等待Actor初始化完成
     * @param ref
     */
    private void waitForActorInitialization(ActorRef<?> ref) {
        // 等待Actor进入RUNNING状态
        long deadline = System.currentTimeMillis() + 5000; // 5秒超时
        while (System.currentTimeMillis() < deadline) {
            if (contextManager.getContext(ref.path()).get().getLifecycle().getState() == LifecycleState.RUNNING) {
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

    /**
     * 创建一个Actor
     * @param props
     * @param name
     * @return
     * @param <T>
     */
    public <T> ActorRef<T> actorOf(Props<T> props, String name) {
        return actorOf(props, name, null);
    }


    public <T> ActorRef<T> actorOf(Props<T> props, String name, ActorContext parentContext) {
        // 验证系统状态和Actor名称
        if (state.get() != SystemState.RUNNING) {
            throw new IllegalStateException("Actor system is not running");
        }

        validateActorName(name);


        // 2. 处理顶级Actor创建
        if (parentContext == null) {
            return createViaUserGuardian(props, name);
        }

        String path = parentContext.getPath() + "/" + name;

        // 检查是否已存在
        if (contextManager.hasContext(path)) {
            throw new ActorCreationException("Actor already exists at path: " + path);
        }

        try {
            // 创建Props
            TypedActor<T> typedActor = props.newActor();
            // 创建一个未类型化的Actor
            UnTypedActor<T> unTypedActor=new UnTypedActor<>(typedActor);
            // 创建上下文
            ActorContext context = new ActorContext(path, this, unTypedActor, null, props);
            // 设置上下文
            unTypedActor.setContext(context);
            // 设置自身引用
            String[] split = path.split("/");
            // 创建ActorRef
            LocalActorRef<T> actorRef = new LocalActorRef<>(unTypedActor,path, split[split.length-1],null);

            context.start();

            logger.debug("Created actor: {}", path);
            return actorRef;
        } catch (Exception e) {
            logger.error("Failed to create actor: {}", name, e);
            throw new ActorCreationException("Failed to create actor: " + name, e);
        }
    }

    /**
     * 通过用户守护者创建Actor
     * @param props
     * @param name
     * @return
     * @param <T>
     */
    private <T> ActorRef<T> createViaUserGuardian(Props<T> props, String name) {
        CompletableFuture<ActorRef> future = new CompletableFuture<>();
        // 通过用户守护者创建Actor
        userGuardian.tell(new UserGuardianActorMessage.CreateUserActor(props, name, future),ActorRef.noSender());
        try {
            // 等待创建完成，设置超时
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ActorCreationException("Actor creation timeout: " + name);
        } catch (Exception e) {
            throw new ActorCreationException("Failed to create actor via user guardian: " + name, e);
        }
    }

    /**
     * 验证Actor名称是否符合规范。
     * my-actor
     * Actor_123
     * validName
     * anotherActorName
     * actor-name-123
     * @param name 要验证的Actor名称
     * @throws IllegalArgumentException 如果名称不符合规范
     */
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


    public CompletableFuture<Void> stop(ActorRef actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        Envelope signalEnvelope = Envelope.builder()
                .message(Signal.POISON_PILL)
                .type(MessageType.SIGNAL)
                .priority(Priority.HIGH)
                .scope(SignalScope.SINGLE)
                .build();
        signalEnvelope.addMetadata("stopFuture", stopFuture);
        //通知关闭
        actor.tell(signalEnvelope,ActorRef.noSender());

        // 设置超时逻辑
        return stopFuture.orTimeout(10, TimeUnit.SECONDS)
                .handle((result, exception) -> {
                    if (exception != null) {
                        if (exception instanceof TimeoutException) {
                            // 输出超时日志
                            logger.warn("Actor {} failed to stop within the specified timeout of 10 seconds.", actor.path());
                            Envelope kill = Envelope.builder()
                                    .message(Signal.KILL)
                                    .priority(Priority.HIGH)
                                    .type(MessageType.SIGNAL)
                                    .scope(SignalScope.SINGLE)
                                    .build();
                            //通知关闭
                            actor.tell(kill, ActorRef.noSender());
                        } else {
                            // 处理其他异常
                            logger.error("An error occurred while stopping actor {}: ", actor.path(), exception.getMessage());
                        }
                        // 如果发生异常，返回一个失败的Future
                        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(exception);
                        return failedFuture;
                    } else {
                        // 如果成功停止，返回原来的Future
                        return stopFuture;
                    }
                })
                .thenCompose(Function.identity()); // 确保返回的Future是正确的
    }



    public CompletableFuture<Void> terminate() {
        if (state.compareAndSet(SystemState.RUNNING, SystemState.TERMINATING)) {
            try {
                logger.info("Terminating actor system '{}'...", name);

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

                logger.info("Actor system '{}' terminated", name);
            } catch (Exception e) {
                logger.error("Error during actor system termination", e);
                terminationFuture.completeExceptionally(e);
            }
        }
        return terminationFuture;
    }

    public void handleSystemFailure(Throwable cause, ActorRef<?> failedActor) {
        try {
            logger.error("System level failure from actor: {}", failedActor.path(), cause);

            // 2. 通知死亡监视器
            deathWatch.signalTermination(failedActor, false);
            // 3. 发送到系统守护者
            systemGuardian.tell(Signal.SYSTEM_FAILURE, ActorRef.noSender());

            // 4. 根据错误严重程度决定是否需要停止 Actor
            if (isSystemFailure(cause)) {
                stopActor(failedActor)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            logger.error("Failed to stop actor after system failure: {}", failedActor.path(), ex);
                            return null;
                        });
            }
        } catch (Exception e) {
            logger.error("Error handling system failure", e);
        }
    }

    private CompletableFuture<Void> stopActor(ActorRef actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        Envelope signalEnvelope = Envelope.builder()
                .message(Signal.POISON_PILL)
                .type(MessageType.SIGNAL)
                .priority(Priority.HIGH)
                .scope(SignalScope.SINGLE)
                .build();
        signalEnvelope.addMetadata("stopFuture", stopFuture);
        //通知关闭
        actor.tell(signalEnvelope,ActorRef.noSender());

        // 设置超时逻辑
        return stopFuture.orTimeout(10, TimeUnit.SECONDS)
                .handle((result, exception) -> {
                    if (exception != null) {
                        if (exception instanceof TimeoutException) {
                            // 输出超时日志
                            logger.warn("Actor {} failed to stop within the specified timeout of 10 seconds.", actor.path());
                            Envelope kill = Envelope.builder()
                                    .message(Signal.KILL)
                                    .priority(Priority.HIGH)
                                    .scope(SignalScope.SINGLE)
                                    .build();
                            //通知关闭
                            actor.tell(kill,ActorRef.noSender());
                        } else {
                            // 处理其他异常
                            logger.error("An error occurred while stopping actor {}: ", actor.path(), exception.getMessage());
                        }
                        // 如果发生异常，返回一个失败的Future
                        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(exception);
                        return failedFuture;
                    } else {
                        // 如果成功停止，返回原来的Future
                        return stopFuture;
                    }
                })
                .thenCompose(Function.identity()); // 确保返回的Future是正确的
    }

    public boolean isSystemFailure(Throwable cause) {
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
            logger.error("Interrupted while shutting down internals", e);
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
        refRegistry.unregister(path);
        contextManager.removeContext(path);
        logger.debug("Unregistered actor: {}", path);
    }

    // Getters
    public String name() {
        return name;
    }

    public Dispatcher dispatcher() {
        return dispatcher;
    }

    public DeathWatch getDeathWatch() {
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

    public EventStream getEventStream() {
        return eventStream;
    }
}
