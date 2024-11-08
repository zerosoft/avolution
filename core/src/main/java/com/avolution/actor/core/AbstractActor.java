package com.avolution.actor.core;


import com.avolution.actor.context.ActorContext;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.exception.AskTimeoutException;
import com.avolution.actor.lifecycle.LifecycleState;
import  com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageHandler;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;
import com.avolution.actor.metrics.ActorMetricsCollector;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public abstract class AbstractActor<T> implements ActorRef<T>, MessageHandler<T> {
    Logger logger=org.slf4j.LoggerFactory.getLogger(AbstractActor.class);
    /**
     * Actor上下文
     */
    protected ActorContext context;

    private volatile Envelope<T> currentMessage;
    /**
     * Actor生命周期状态
     */
    protected LifecycleState lifecycleState = LifecycleState.NEW;
    // 持有唯一的ActorRefProxy引用
    private ActorRefProxy<T> selfRef;
    /**
     * 消息处理器
     */
    private final Map<Class<?>, Consumer<Object>> handlers = new HashMap<>();

    // 死信相关字段
    private final ConcurrentLinkedQueue<IDeadLetterActorMessage.DeadLetter> deadLetters = new ConcurrentLinkedQueue<>();
    private static final int MAX_DEAD_LETTERS = 1000;
    private final AtomicInteger deadLetterCount = new AtomicInteger(0);

    private ActorMetricsCollector metricsCollector;

    public AbstractActor() {
        registerHandlers();
    }

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
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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


    protected void setSelfRef(ActorRefProxy<T> ref) {
        if (this.selfRef == null) {
            this.selfRef = ref;
        }
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
        if (!isTerminated()) {
            Envelope<T> envelope=new Envelope<>(message,sender,this,MessageType.NORMAL,1);
            context.tell(envelope);
        }
    }

    @Override
    public void tell(Signal message, ActorRef sender) {
        if (!isTerminated()) {
            Envelope envelope=new Envelope(message,sender,this,MessageType.SYSTEM,1);
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
        return lifecycleState == LifecycleState.STOPPED;
    }

    // Actor正式初始换之前的
    public void preStart() {

    }
    // 生命周期回调方法
    public void postStop() {
        try {
            // 执行子类的清理逻辑
            onPostStop();
        } finally {
            // 确保资源被清理
            destroy();
        }
    }

    /**
     * 子类可以重写此方法实现自定义清理逻辑
     */
    protected void onPostStop() {
        // 默认实现为空
    }

    public void preRestart(Throwable reason) {
        // 在重启之前的处理逻辑
    }

    public void postRestart(Throwable reason) {
        // 在重启之后的处理逻辑
    }

    public void initialize(ActorContext context) {
        this.context = context;
        this.lifecycleState = LifecycleState.STARTED;
        this.metricsCollector = new ActorMetricsCollector(path());
    }

    @Override
    public void handle(Envelope<T> message) throws Exception {

        if (isTerminated()) {
            handleDeadLetter(message);
            return;
        }

        long startTime = System.nanoTime();
        boolean success = true;

        try {
            // 记录消息处理时间
            currentMessage = message;
            // 处理系统信号
            if (message.getMessage() instanceof Signal signal) {
                signal.handle(this);
            }else {
                onReceive(currentMessage.getMessage());
            }
        } catch (Exception e) {
            success=false;
            context.handleFailure(e, message);
        } finally {
            currentMessage = null;
            if (metricsCollector != null) {
                metricsCollector.recordMessage(startTime, success);
            }
        }
    }

    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        CompletableFuture<R> future = new CompletableFuture<>();

        // 创建临时Actor接收响应
        Props<R> tempProps = Props.create(() -> new AbstractActor<R>() {
            @Override
            public void onReceive(R message) {
                future.complete(message);
                context.stop();
            }
        });

        // 创建临时Actor
        ActorRef<R> tempActor = context.actorOf(tempProps, "temp-" + UUID.randomUUID());

        // 发送消息
        tell(message, tempActor);

        // 设置超时
        context.system().scheduler().schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(
                        new AskTimeoutException("Ask timed out after " + timeout)
                );
                context.stop(tempActor);
            }
        }, 5L, TimeUnit.SECONDS);

        return future;
    }

    public <R> CompletableFuture<R> ask(T message) {
        return ask(message, Duration.ofSeconds(5)); // 默认5秒超时
    }

    /**
     * 销毁Actor的资源
     */
    protected void destroy() {
        // 1. 取消所有待处理的消息
        if (currentMessage != null) {
            currentMessage = null;
        }

        // 2. 清理上下文引用
        if (context != null) {
            // 停止所有子Actor
            context.getChildren().values().forEach(child -> {
                context.system().stop(child);
            });
            context = null;
        }

        // 3. 更新生命周期状态
        lifecycleState = LifecycleState.STOPPED;

        // 清理死信队列
        deadLetters.clear();
        deadLetterCount.set(0);

        // 如果有未处理的当前消息，将其作为死信处理
        if (currentMessage != null) {
            handleDeadLetter(currentMessage);
            currentMessage = null;
        }
    }


    /**
     * 优雅关闭Actor
     */
    public final void shutdown() {
        if (lifecycleState != LifecycleState.STOPPED) {
            try {
                // 1. 调用预停止钩子
                postStop();

                // 2. 停止接收新消息
                lifecycleState = LifecycleState.STOPPING;

                // 3. 等待当前消息处理完成
                if (currentMessage != null) {
                    // 可以添加超时逻辑
                    while (currentMessage != null) {
                        Thread.yield();
                    }
                }
                // 4. 销毁资源
                postStop();

            } catch (Exception e) {
                logger.error("Error during actor shutdown: {}", e.getMessage(), e);
            } finally {
                // 5. 确保状态更新
                lifecycleState = LifecycleState.STOPPED;
            }
        }
    }
}
