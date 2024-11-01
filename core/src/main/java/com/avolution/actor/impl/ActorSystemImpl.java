package com.avolution.actor.impl;

import com.avolution.actor.config.ActorSystemConfig;
import com.avolution.actor.core.*;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.exception.SystemInitializationException;
import com.avolution.actor.extension.Extension;
import com.avolution.actor.extension.ExtensionId;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.message.SystemMessage;
import com.avolution.actor.routing.RouterManager;
import com.avolution.actor.supervision.DeathWatch;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.system.actor.DeadLetterActor;
import com.avolution.actor.system.actor.SystemGuardian;
import com.avolution.actor.system.actor.UserGuardian;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Actor系统实现类
 */
public class ActorSystemImpl implements ActorSystem {
    private static final Logger log = LoggerFactory.getLogger(ActorSystemImpl.class);

    private final String name;
    private final ActorSystemConfig config;
    private final Dispatcher dispatcher;
    private final DeathWatch deathWatch;
    private final RouterManager routerManager;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ActorRef> actors;
    private final Map<ExtensionId<?>, Extension> extensions;
    private final AtomicReference<SystemState> state;
    private final CompletableFuture<Void> terminationFuture;
    private final ActorRef deadLetters;
    private final ActorRef systemGuardian;
    private final ActorRef userGuardian;
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    public ActorSystemImpl(String name, ActorSystemConfig config) {
        this.name = name;
        this.config = config;
        this.dispatcher = new Dispatcher(config.getDispatcherConfig());
        this.deathWatch = new DeathWatch(this);
        this.routerManager = new RouterManager(this);
        this.scheduler = Executors.newVirtualThreadPerTaskExecutor();
        this.actors = new ConcurrentHashMap<>();
        this.extensions = new ConcurrentHashMap<>();
        this.state = new AtomicReference<>(SystemState.INITIALIZING);
        this.terminationFuture = new CompletableFuture<>();

        // 创建系统Actor
        this.deadLetters = createDeadLetterActor();
        this.systemGuardian = createSystemGuardian();
        this.userGuardian = createUserGuardian();

        initialize();
    }

    private void initialize() {
        try {
            // 初始化内置扩展
            initializeBuiltInExtensions();

            // 启动系统Actor
            startSystemActors();

            state.set(SystemState.RUNNING);
            log.info("Actor system '{}' started", name);
        } catch (Exception e) {
            state.set(SystemState.TERMINATED);
            log.error("Failed to initialize actor system '{}'", name, e);
            throw new SystemInitializationException("Failed to initialize actor system", e);
        }
    }

    // 初始化内置扩展
    private void initializeBuiltInExtensions() {

    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ActorRef actorOf(Props props, String name) {
        checkRunning();
        validateActorName(name);

        String path = "/user/" + name;
        try {
            ActorRef actor = props.create(this, path, userGuardian);
            ActorRef existing = actors.putIfAbsent(path, actor);

            if (existing != null) {
                throw new ActorCreationException("Actor '" + name + "' already exists");
            }

            // 启动Actor
            actor.tell(new Start(), ActorRef.noSender());
            log.debug("Created actor: {}", path);
            return actor;
        } catch (Exception e) {
            log.error("Failed to create actor: {}", name, e);
            throw new ActorCreationException("Failed to create actor: " + name, e);
        }
    }

    @Override
    public Optional<ActorRef> findActor(String path) {
        checkRunning();
        return Optional.ofNullable(actors.get(path));
    }

    @Override
    public List<ActorRef> getChildActors(String parentPath) {
        checkRunning();
        return actors.entrySet().stream()
            .filter(e -> {
                String path = e.getKey();
                return path.startsWith(parentPath + "/") &&
                       path.substring(parentPath.length() + 1).indexOf('/') == -1;
            })
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public ActorRef createTempActor(Props props) {
        String tempName = "temp-" + UUID.randomUUID().toString();
        ActorRef actor = actorOf(props, tempName);
        // 设置Actor在完成后自动销毁
        actor.tell(new AutoDestroyMessage(), ActorRef.noSender());
        return actor;
    }

    @Override
    public Dispatcher dispatcher() {
        return dispatcher;
    }

    @Override
    public DeathWatch deathWatch() {
        return deathWatch;
    }

    @Override
    public RouterManager router() {
        return routerManager;
    }

    @Override
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public ActorRef deadLetters() {
        return deadLetters;
    }

    @Override
    public <T extends Extension> T extension(ExtensionId<T> extensionId) {
        checkRunning();
        return (T) extensions.computeIfAbsent(extensionId, id -> {
            T extension = id.createExtension(this);
            extension.initialize();
            return extension;
        });
    }

    @Override
    public CompletionStage<Void> terminate() {
        if (state.compareAndSet(SystemState.RUNNING, SystemState.TERMINATING)) {
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("Terminating actor system '{}'", name);

                    // 停止用户Actor
                    stopUserActors();

                    // 停止系统Actor
                    stopSystemActors();

                    // 关闭调度器和扩展
                    shutdownInternals();

                    state.set(SystemState.TERMINATED);
                    terminationFuture.complete(null);
                    log.info("Actor system '{}' terminated", name);
                } catch (Exception e) {
                    log.error("Failed to terminate actor system '{}'", name, e);
                    terminationFuture.completeExceptionally(e);
                }
            });
        }
        return terminationFuture;
    }

    @Override
    public void awaitTermination(Duration timeout) throws InterruptedException {
        try {
            terminationFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("System termination timed out", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("System termination failed", e.getCause());
        }
    }

    @Override
    public ActorSystemConfig getConfig() {
        return config;
    }

    @Override
    public SystemState getState() {
        return state.get();
    }

    @Override
    public boolean isRunning() {
        return state.get() == SystemState.RUNNING;
    }

    @Override
    public SystemStatus getSystemStatus() {
        return new SystemStatus(
            state.get(),
            actors.size(),
            dispatcher.getActiveCount(),
            ((ThreadPoolExecutor) scheduler).getActiveCount()
        );
    }

    @Override
    public ActorStats getActorStats() {
        Map<String, Integer> actorTypeCount = new HashMap<>();
        actors.values().forEach(actor -> {
            String type = actor.getClass().getSimpleName();
            actorTypeCount.merge(type, 1, Integer::sum);
        });

        return new ActorStats(
            actors.size(),
            actorTypeCount,
            messageCount.get(),
            errorCount.get()
        );
    }

    // 内部辅助方法
    private void checkRunning() {
        if (state.get() != SystemState.RUNNING) {
            throw new IllegalStateException("Actor system is not running");
        }
    }

    private void validateActorName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Actor name cannot be null or empty");
        }
        if (!name.matches("[a-zA-Z0-9-_]+")) {
            throw new IllegalArgumentException("Invalid actor name: " + name);
        }
    }

    private ActorRef createDeadLetterActor() {
        Props props = Props.create(DeadLetterActor.class);
        return createActor(props, "/deadLetters", null);
    }

    private ActorRef createSystemGuardian() {
        Props props = Props.create(SystemGuardian.class);
        return createActor(props, "/system", null);
    }

    private ActorRef createUserGuardian() {
        Props props = Props.create(UserGuardian.class);
        return createActor(props, "/user", systemGuardian);
    }

    private ActorRef createActor(Props props, String path, ActorRef parent) {
        try {
            AbstractActor actor = props.createActor();
            actor.initialize(createContext(actor, path, parent), path);
            actors.put(path, actor);
            return actor;
        } catch (Exception e) {
            log.error("Failed to create actor: {}", path, e);
            throw new ActorCreationException("Failed to create actor: " + path, e);
        }
    }


    private void startSystemActors() {
        systemGuardian.tell(new SystemMessage.Start(), ActorRef.noSender());
        userGuardian.tell(new SystemMessage.Start(), ActorRef.noSender());
    }

    private void stopUserActors() {
        List<ActorRef> userActors = actors.values().stream()
            .filter(ref -> ref.path().startsWith("/user/"))
            .collect(Collectors.toList());

        userActors.forEach(actor -> actor.tell(PoisonPill.INSTANCE, ActorRef.noSender()));
        awaitActorsTermination(userActors, config.getShutdownTimeout());
    }

    private void stopSystemActors() {
        systemGuardian.tell(PoisonPill.INSTANCE, ActorRef.noSender());
        userGuardian.tell(PoisonPill.INSTANCE, ActorRef.noSender());
        awaitActorsTermination(
            Arrays.asList(systemGuardian, userGuardian),
            Duration.ofSeconds(5)
        );
    }

    private void shutdownInternals() {
        // 关闭扩展
        extensions.values().forEach(Extension::shutdown);
        extensions.clear();

        // 关闭调度器
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 关闭分发器
        dispatcher.shutdown(Duration.ofSeconds(5L));
    }

    private void awaitActorsTermination(List<ActorRef> actors, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (!actors.isEmpty() && System.currentTimeMillis() < deadline) {
            actors.removeIf(actor -> !this.actors.containsKey(actor.path()));
            if (!actors.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }


}