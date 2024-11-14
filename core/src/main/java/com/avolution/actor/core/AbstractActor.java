package com.avolution.actor.core;


import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.lifecycle.LifecycleState;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageHandler;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;
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
import java.util.concurrent.CompletionException;
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
    protected void handleDeadLetter(Envelope<?> envelope) {
        if (context == null || context.system() == null) {
            return;
        }

        IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                envelope.getMessage(),
                envelope.getSender().path(),
                envelope.getRecipient().path(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                envelope.getMessageType().toString(),
                envelope.getRetryCount()
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
            Envelope<T> envelope=new Envelope<>(message,sender,this,MessageType.NORMAL,1);
            context.tell(envelope);
        }
    }

    @Override
    public void tell(Signal message, ActorRef sender) {
        if (!isTerminated()) {
            Envelope<?> envelope=new Envelope(message,sender,this,MessageType.SYSTEM,1);
            context.tell(envelope);
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
    public void handle(Envelope<T> message) throws Exception {
        // 检查是否已经处理过该消息
        if (message.hasBeenProcessedBy(path())) {
            logger.warn("Detected circular message delivery: {} in actor: {}", message.getMessage().getClass().getSimpleName(), path());
            handleDeadLetter(message);
            return;
        }

        if (isTerminated()) {
            logger.warn("Actor is terminated, cannot process message: {}", message.getMessage().getClass().getSimpleName());
            handleDeadLetter(message);
            return;
        }

        // 标记消息已被当前 Actor 处理
        message.markProcessed(path());

        long startTime = System.nanoTime();
        boolean success = true;

        currentProcessingThread = Thread.currentThread();
        try {
            // 处理消息
            strategy.beforeMessageHandle(message, this);
            currentMessage = message;
            strategy.handleMessage(message, this);
        } catch (Exception e) {
            success = false;
            strategy.handleFailure(e, message, this);
        } finally {
            currentMessage = null;
            strategy.afterMessageHandle(message, this, success);
            if (metricsCollector != null) {
                metricsCollector.recordMessage(startTime, success);
            }
            currentProcessingThread=null;
        }

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
    protected void interruptCurrentProcessing() {
        Thread processingThread = currentProcessingThread;
        if (processingThread != null) {
            processingThread.interrupt();
            currentProcessingThread = null;
        }
        currentMessage = null;
    }

    @Override
    protected void doCleanup() {

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
