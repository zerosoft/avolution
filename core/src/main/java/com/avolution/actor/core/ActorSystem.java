package com.avolution.actor.core;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import com.avolution.actor.context.ActorContextManager;
import com.avolution.actor.context.ActorRefRegistry;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.exception.ActorStopException;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.supervision.DeathWatch;
import com.avolution.actor.context.ActorContext;
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

    private <T> ActorRef<T> createSystemActor(Class<? extends AbstractActor<T>> actorClass, String path) {
        return actorOf(Props.create(actorClass), path);
    }

    private void start() {
        if (state.compareAndSet(SystemState.NEW, SystemState.RUNNING)) {
            log.info("Actor system '{}' started", name);
        }
        // 创建系统Actor
        this.deadLetters = createSystemActor(DeadLetterActor.class, "/system/deadLetters");
        this.systemGuardian = createSystemActor(SystemGuardianActor.class, "/system/guardian");
        this.userGuardian = createSystemActor(UserGuardianActor.class, "/user");
    }

    public <T> ActorRef<T> actorOf(Props<T> props, String name) {
        return actorOf(props, name, null);
    }


    private final Map<String,AtomicInteger> pathId = new HashMap<>();

    public int generateUniqueId(String path) {
        AtomicInteger atomicInteger = pathId.computeIfAbsent(path, k -> new AtomicInteger(0));
        return atomicInteger.incrementAndGet();
    }

    public <T> ActorRef<T> actorOf(Props<T> props, String name,ActorContext actorContextRef) {
        // 验证系统状态和Actor名称
        if (state.get() != SystemState.RUNNING) {
            throw new IllegalStateException("Actor system is not running");
        }

//        validateActorName(name);
        String path;
        if (actorContextRef!=null){
            path =  name +"#"+generateUniqueId(name);
        }else {
             path = "/user/" + name +"#"+generateUniqueId(name);
        }


        // 检查是否已存在
        if (contextManager.hasContext(path)) {
            throw new ActorCreationException("Actor already exists at path: " + path);
        }

        try {
            AbstractActor<T> actor = props.newActor();
            ActorContext context = new ActorContext(path, this, actor, actorContextRef != null ? actorContextRef : null, props);

            LocalActorRef<T> actorRef = new LocalActorRef<>(actor);

            if (actorContextRef!=null){
                actorContextRef.getChildren().put(name,actorRef);
            }
            // 初始化Actor
            actor.initialize(context);
            // 注册Actor
            registerActor(actorRef,context);

            log.debug("Created actor: {}", path);
            return actorRef;
        } catch (Exception e) {
            log.error("Failed to create actor: {}", name, e);
            throw new ActorCreationException("Failed to create actor: " + name, e);
        }
    }

    public void stop(ActorRef actorRef) {
        actorRef.tell(PoisonPill.INSTANCE,ActorRef.noSender());
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
        refRegistry.register(ref,ref.path());
        contextManager.addContext(ref.path(), context);
    }

    public void unregisterActor(String path) {
        refRegistry.unregister(path);
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
}
