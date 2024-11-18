package com.avolution.actor.core;


import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.lifecycle.LifecycleState;
import com.avolution.actor.message.*;
import com.avolution.actor.metrics.ActorMetrics;
import com.avolution.actor.metrics.ActorMetricsCollector;
import com.avolution.actor.pattern.AskPattern;
import com.avolution.actor.strategy.ActorStrategy;
import com.avolution.actor.strategy.DefaultActorStrategy;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public abstract class AbstractActor<T>  extends ActorLifecycle implements ActorRef<T>, MessageHandler<T> {
    Logger logger=org.slf4j.LoggerFactory.getLogger(AbstractActor.class);

    private static final int MAX_DEAD_LETTERS = 1000;
    /**
     * Actor上下文
     */
    protected ActorContext context;
    /**
     * 当前处理的消息
     */
    private volatile Envelope<T> currentMessage;

    // 持有唯一的ActorRefProxy引用
    private LocalActorRef<T> selfRef;
    /**
     * 消息处理器
     */
    private final Map<Class<?>, Consumer<Object>> handlers = new HashMap<>();

    // 死信相关字段
    private final ConcurrentLinkedQueue<IDeadLetterActorMessage.DeadLetter> deadLetters = new ConcurrentLinkedQueue<>();
    /**
     * 死信计数
     */
    private final AtomicInteger deadLetterCount = new AtomicInteger(0);
    /**
     * 消息度量收集器
     */
    private ActorMetricsCollector metricsCollector;
    /**
     * Actor策略
     */
    private ActorStrategy<T> strategy = new DefaultActorStrategy<>();
    /**
     * 当前消息处理线程
     */
    private volatile Thread currentProcessingThread;

    private volatile StopReason stopReason;

    private void registerHandlers() {
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnReceive.class)) {
                Class<?> messageType = method.getAnnotation(OnReceive.class).value();
                if (method.getParameterCount() == 1 && messageType.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    handlers.put(messageType, message -> invokeHandler(method, message));
                }
            }
        }
    }

    private void invokeHandler(Method method, Object message) {
        try {
            method.invoke(this, message);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            logger.error("Error invoking message handler for {}: {}", message.getClass().getSimpleName(), cause.getMessage());
            strategy.handleFailure(cause, currentMessage, this);
        } catch (Exception e) {
            logger.error("Error invoking message handler", e);
            strategy.handleFailure(e, currentMessage, this);
        }
    }


    public void initialize() {
        try {
            // 1. 注册消息处理器
            registerHandlers();

            // 2. 验证Actor配置
            validateConfiguration();

            // 3. 初始化策略
            initializeStrategy();

            // 4. 启动度量收集
            startMetricsCollection();

            logger.debug("Actor initialized: {}", context.getPath());
        } catch (Exception e) {
            logger.error("Failed to initialize actor: {}", context.getPath(), e);
            throw new ActorInitializationException("Actor initialization failed", e);
        }
    }

    private void validateConfiguration() {
        if (context == null) {
            throw new ActorInitializationException("Actor context not set");
        }
        if (selfRef == null) {
            throw new ActorInitializationException("Actor reference not set");
        }
    }

    private void initializeStrategy() {
        if (strategy == null) {
            strategy = new DefaultActorStrategy<>();
        }
    }

    private void startMetricsCollection() {
        if (metricsCollector == null) {
            metricsCollector = new ActorMetricsCollector(path());
        }
    }
    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     */
    public void onReceive(T message) {
        Consumer<Object> handler = handlers.get(message.getClass());
        if (handler != null) {
            handler.accept(message);
        } else {
            unhandled(message);
        }
    }

    public void unhandled(T message) {
        System.out.println("Unhandled message: " + message);
    }

    /**
     * 处理死信消息
     */
    public void handleDeadLetter(Envelope<?> envelope) {
        if (context == null || context.system() == null) {
            return;
        }

        IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                envelope.getMessage(),
                envelope.getSender().path(),
                envelope.getRecipient().path(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                envelope.getMessageType(),
                envelope.getRetryCount(),new HashMap<>()
        );

        // 记录死信
        logger.warn("Dead letter received: {}", deadLetter);

        // 维护死信队列
        deadLetters.offer(deadLetter);
        if (deadLetters.size() > MAX_DEAD_LETTERS) {
            deadLetters.poll();
        }

        // 统计
        int count = deadLetterCount.incrementAndGet();
        if (count % 100 == 0) {
            logger.warn("Dead letter count reached: {}", count);
        }

        // 发送到系统的死信Actor
        context.system().getDeadLetters().tell(deadLetter, getSelf());
    }


    /**
     * 获取消息发送者
     * @return
     */
    public ActorRef getSender() {
        return currentMessage.getSender();
    }


    public void setSelfRef(LocalActorRef<T> ref) {
        if (this.selfRef != null) {
            throw new IllegalStateException("Self reference already set");
        }
        this.selfRef = ref;
    }

    public ActorRef<T> getSelf() {
        return selfRef;
    }
    /**
     * 获取Actor上下文
     */
    public ActorContext getContext() {
        return context;
    }

    @Override
    public void tell(T message, ActorRef sender) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!isTerminated()) {
            Envelope<T> envelope=new Envelope<>(message,sender,this,MessageType.NORMAL,0);
            context.tell(envelope);
        }
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (!isTerminated()) {
            SignalEnvelope envelope = createSignalEnvelope(signal, sender);
            tell(envelope);
        }
    }


    public void tell(SignalEnvelope envelope) {
        if (!isTerminated()) {
            context.tell(envelope);
        }
    }

    private SignalEnvelope createSignalEnvelope(Signal signal, ActorRef sender) {
        if (signal == Signal.KILL || signal == Signal.ESCALATE) {
            return new SignalEnvelope(signal, sender, (ActorRef<Signal>) getSelf(),
                    SignalPriority.HIGH, SignalScope.SINGLE);
        } else if (signal.isLifecycleSignal()) {
            return signal.envelope(sender, (ActorRef<Signal>) getSelf());
        } else {
            return new SignalEnvelope(signal, sender, (ActorRef<Signal>) getSelf(),
                    SignalPriority.LOW, SignalScope.SINGLE);
        }
    }

    @Override
    public String path() {
        return context.getPath();
    }

    @Override
    public String name() {
        String path = path();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public boolean isTerminated() {
        return isShuttingDown()|| getLifecycleState() != LifecycleState.RUNNING;
    }


    @Override
    public void handle(Envelope<T> envelope) throws Exception {
        currentMessage = envelope;
        currentProcessingThread = Thread.currentThread();

        try {
            T message = envelope.getMessage();
            Class<?> messageClass = message.getClass();

            Consumer<Object> handler = handlers.get(messageClass);
            if (handler != null) {
                strategy.beforeMessageHandle(envelope, this);
                handler.accept(message);
                strategy.afterMessageHandle(envelope, this, true);
            } else {
                unhandled(message);
            }
        } catch (Exception e) {
            strategy.handleFailure(e, envelope, this);
            throw e;
        } finally {
            currentMessage = null;
            currentProcessingThread = null;
        }
    }

    @Override
    public void handleSignal(Signal signal) throws Exception {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }

        // 创建一个特殊的信号包装器
        @SuppressWarnings("unchecked")
        Envelope<Signal> envelope = new Envelope<>(
                signal,
                getSelf(),
                (ActorRef<Signal>) getSelf(),MessageType.SYSTEM,0
        );

        try {
            // 使用正确的类型参数调用策略方法
            @SuppressWarnings("unchecked")
            ActorStrategy<Signal> signalStrategy = (ActorStrategy<Signal>) strategy;
            signalStrategy.beforeMessageHandle(envelope, (AbstractActor<Signal>) this);

            // 记录信号处理开始
            metricsCollector.incrementSignalCount();

            switch (signal.getType()) {
                case LIFECYCLE -> handleLifecycleSignal(signal);
                case CONTROL -> handleControlSignal(signal);
                case SUPERVISION -> handleSupervisionSignal(signal);
                case QUERY -> handleQuerySignal(signal);
                default -> {
                    logger.warn("Unhandled signal type received: {} in actor: {}", signal.getType(), path());
                    unhandled((T) signal);
                }
            }

            signalStrategy.afterMessageHandle(envelope, (AbstractActor<Signal>) this, true);
        } catch (Exception e) {
            metricsCollector.incrementFailureCount();
            @SuppressWarnings("unchecked")
            ActorStrategy<Signal> signalStrategy = (ActorStrategy<Signal>) strategy;
            signalStrategy.handleFailure(e, envelope, (AbstractActor<Signal>) this);
            throw e;
        }
    }

    private void handleLifecycleSignal(Signal signal) {
        try {
            switch (signal) {
                case START -> {
                    logger.debug("Handling START signal in actor: {}", path());
                    start();
                }
                case STOP -> {
                    logger.debug("Handling STOP signal in actor: {}", path());
                    stopReason = StopReason.SIGNAL_STOP;
                    stop();
                }
                case RESTART -> {
                    logger.debug("Handling RESTART signal in actor: {}", path());
                    stopReason = StopReason.RESTART;
                    doRestart(null);
                }
                default -> throw new IllegalArgumentException("Unknown lifecycle signal: " + signal);
            }
        } catch (Exception e) {
            logger.error("Error handling lifecycle signal: {} in actor: {}", signal, path(), e);
            throw e;
        }
    }

    private void handleControlSignal(Signal signal) {
        try {
            switch (signal) {
                case SUSPEND -> {
                    context.suspend();
                    logger.debug("Actor suspended: {}", path());
                }
                case RESUME -> {
                    context.resume();
                    logger.debug("Actor resumed: {}", path());
                }
                case POISON_PILL -> {
                    stopReason = StopReason.POISON_PILL;
                    stop();
                    logger.info("Actor poisoned: {}", path());
                }
                case KILL -> {
                    stopReason = StopReason.KILLED;
                    stop();
                    logger.info("Actor KILL: {}", path());
                }
                default -> logger.warn("Unknown control signal: {} in actor: {}", signal, path());
            }
        } catch (Exception e) {
            logger.error("Error handling control signal: {} in actor: {}", signal, path(), e);
            throw e;
        }
    }

    private void handleSupervisionSignal(Signal signal) {
        switch (signal) {
            case ESCALATE -> context.escalate(null);
            case SUPERVISE -> strategy.handleFailure(null, getCurrentMessage(), this);
        }
    }

    private void handleQuerySignal(Signal signal) {
        try {
            switch (signal) {
                case STATUS -> {
                    ActorStatus status = new ActorStatus(
                            path(),
                            getLifecycleState(),
                            context.getChildren().size(),
                            currentMessage != null
                    );
                    // 修复: 使用正确的类型转换
                    context.tell(new Envelope<>(
                            (T) status,
                            getSelf(),
                            (ActorRef<T>) getSelf(),
                            MessageType.NORMAL,
                            0
                    ));
                }
                case METRICS -> {
                    ActorMetrics metrics = metricsCollector.getMetrics();
                    // 修复: 使用正确的类型转换
                    context.tell(new Envelope<>(
                            (T) metrics,
                            getSelf(),
                            (ActorRef<T>) getSelf(),
                            MessageType.NORMAL,
                            0
                    ));
                }
                default -> throw new IllegalArgumentException("Unknown query signal: " + signal);
            }
        } catch (Exception e) {
            logger.error("Error handling query signal: {} in actor: {}", signal, path(), e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<Void> waitForMessageProcessing() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            // 1. 立即中断当前处理线程
            interruptCurrentProcessing();
            // 2. 完成future
            future.complete(null);
        } catch (Exception e) {
            logger.error("Error during force message processing termination: {}", context.getPath(), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void interruptCurrentProcessing() {
        Thread processingThread = currentProcessingThread;
        if (processingThread != null) {
            processingThread.interrupt();
            currentProcessingThread = null;
        }
        currentMessage = null;
    }

    @Override
    public Envelope<T> getCurrentMessage() {
        return currentMessage;
    }


    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        return AskPattern.ask(
                this,
                timeout,
                replyTo -> message
        );
    }

    public <R> CompletableFuture<R> ask(T message) {
        return ask(message, Duration.ofSeconds(5)); // 默认5秒超时
    }


    @Override
    protected void doStart() {
        onPreStart();
        initialize();
        onPostStart();
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            // 在当前线程等待消息处理完成
            waitForMessageProcessing().get(1, TimeUnit.SECONDS);

            // 执行停止回调
            onPreStop();

            // 记录停止原因
            if (stopReason == null) {
                stopReason = StopReason.SELF_STOP;
            }

            // 清理资源
            doCleanup();

            // 执行停止后回调
            onPostStop();

            future.complete(null);
        } catch (Exception e) {
            logger.error("Error during stop", e);
            stopReason = StopReason.ERROR_STOP;
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 由父Actor或系统调用的停止方法
     */
    public CompletableFuture<Void> stopByParent() {
        stopReason = StopReason.PARENT_STOP;
        return stop();
    }

    /**
     * 由系统调用的停止方法
     */
    public CompletableFuture<Void> stopBySystem() {
        stopReason = StopReason.SYSTEM_STOP;
        return stop();
    }

    /**
     * 由监督策略调用的停止方法
     */
    public CompletableFuture<Void> stopBySupervision() {
        stopReason = StopReason.SUPERVISION_STOP;
        return stop();
    }

    /**
     * 获取停止原因
     */
    public StopReason getStopReason() {
        return stopReason;
    }

    /**
     * 判断是否由父Actor停止
     */
    public boolean isStoppedByParent() {
        return StopReason.PARENT_STOP.equals(stopReason);
    }



    @Override
    protected void doRestart(Throwable reason) {
        onPreRestart(reason);
        stop().join();
        start();
        onPostRestart(reason);
    }



    @Override
    protected void doCleanup() {
        context.stop();
    }

    public void setContext(ActorContext context) {
        if (this.context != null) {
            throw new IllegalStateException("Context already set");
        }
        this.context = context;
    }

    public void setStrategy(ActorStrategy<T> strategy) {
        this.strategy = strategy;
    }


    // 默认生命周期回调实现
    @Override
    protected void onPreStart() {
        if (strategy != null) {
            strategy.onPreStart(this);
        }
    }

    @Override
    protected void onPostStart() {

    }

    // 生命周期回调函数
   public  void preStart() {
        if (strategy != null) {
            strategy.onPreStart(this);
        }
    }

    protected void onPreStop() {
        if (strategy != null) {
            strategy.onPreStop(this);
        }
    }

    protected void onPostStop() {
        if (strategy != null) {
            strategy.onPostStop(this);
        }
    }

    public void onPreRestart(Throwable reason) {
        if (strategy != null) {
            strategy.onPreRestart(reason, this);
        }
    }

    public void onPostRestart(Throwable reason) {
        if (strategy != null) {
            strategy.onPostRestart(reason, this);
        }
    }
}
