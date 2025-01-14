package com.avolution.actor.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.core.context.ActorContextView;
import com.avolution.actor.core.lifecycle.ActorLifecycleHook;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Priority;
import com.avolution.actor.message.Signal;
import com.avolution.actor.pattern.ASK;
import com.avolution.actor.core.strategies.PriorityStrategy;
import com.avolution.actor.core.strategies.RetryStrategy;
import com.avolution.actor.core.strategies.StashStrategy;
import com.avolution.actor.core.strategies.impl.DefaultPriorityStrategy;
import com.avolution.actor.core.strategies.impl.DefaultRetryStrategy;
import com.avolution.actor.core.strategies.impl.DefaultStashStrategy;
import com.avolution.actor.message.ActorFailure;


/**
 * Actor抽象基类，提供基础实现
 * @param <T> Actor可处理的消息类型
 */
public class UnTypedActor<T> implements ActorLifecycleHook,ActorRef<T> {

    Logger logger=org.slf4j.LoggerFactory.getLogger(UnTypedActor.class);
    /**
     * Actor上下文
     */
    protected ActorContext context;

    // 消息发送者
    private ActorRef sender=ActorRef.noSender();

    // 持有唯一的ActorRefProxy引用
    private LocalActorRef<T> selfRef;

    // 实现业务的TypedActor
    private TypedActor<T> typedActor;

    // 添加策略相关属性
    private final PriorityStrategy priorityStrategy;
    private final RetryStrategy retryStrategy; 
    private final StashStrategy stashStrategy;

    public UnTypedActor(TypedActor<T> typedActor) {
        this.typedActor = typedActor;
        // 初始化默认策略
        this.priorityStrategy = new DefaultPriorityStrategy();
        this.retryStrategy = new DefaultRetryStrategy();
        this.stashStrategy = new DefaultStashStrategy();
    }


    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     */
    public void onReceive(Envelope message) {
        try {
            logger.debug("onReceive message:{}",message);
            typedActor.receive(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取消息发送者
     * @return
     */
    public ActorRef getSender() {
        return sender;
    }

    public void setSender(ActorRef sender) {
        this.sender = sender;
    }

    /**
     * 设置Actor引用
     */
    public void setSelfRef(LocalActorRef<T> selfRef) {
        this.selfRef = selfRef;
    }

    /**
     * 获取Actor引用
     */
    public LocalActorRef<T> getSelfRef() {
        return selfRef;
    }

    /**
     * 获取Actor上下文
     */
    public ActorContext getContext() {
        return context;
    }

    /**
     * 设置Actor上下文
     */
    public void setContext(ActorContext context) {
        this.context = context;
    }

    public TypedActor<T> getTypedActor() {
        return typedActor;
    }

    public void setTypedActor(TypedActor<T> typedActor) {
        if (this.typedActor != null) {
            throw new IllegalStateException("TypedActor already set");
        }
        this.typedActor = typedActor;
    }

    /**
     * 发送消息
     * @param message 消息
     * @param sender 发送者
     */
    @Override
    public void tell(T message, ActorRef sender) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (!isTerminated()) {
            logger.debug("tell message:{}",message);
            if (message instanceof Envelope signalEnvelope) {
                context.tell(signalEnvelope);
                logger.debug("tell signalEnvelope:{}",signalEnvelope);
            }else {
                Envelope.Builder builder = Envelope.builder();
                builder.message(message);
                builder.sender(sender);
                builder.recipient(this.getSelfRef());
                builder.type(MessageType.NORMAL);
                builder.retryCount(0);
                Envelope envelope = builder.build();
                context.tell(envelope);
                logger.debug("tell envelope:{}",envelope);
            }

        }else {
            Envelope.Builder builder = Envelope.builder();
            builder.message(message);
            builder.sender(sender);
            builder.recipient(this.getSelfRef());
            builder.type(MessageType.NORMAL);
            builder.retryCount(0);
            Envelope envelope = builder.build();
            IDeadLetterActorMessage.DeadLetter deadLetter = IDeadLetterActorMessage.messageToDeadLetter(envelope);
            // 记录死信
            logger.warn("Dead letter received: {}", deadLetter);

            // 发送到系统的死信Actor
            getContext().getActorSystem().getDeadLetters().tell(deadLetter, getSelfRef());
            logger.warn("Actor is terminated, message not sent: {}", message);
        }
    }
    /**
     * 发送信号
     * @param signal 信号
     * @param sender 发送者
     */
    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (!isTerminated()) {
            Envelope envelope = createSignalEnvelope(signal, sender);
            tell(envelope);
            logger.debug("tell signalEnvelope:{}",envelope);
        }
    }

    /**
     * 发送消息
     * @param envelope 消息
     */
    public void tell(Envelope envelope) {
        if (!isTerminated()) {
            context.tell(envelope);
            logger.debug("tell envelope:{}",envelope);
        }
    }

    /**
     * 创建信号消息
     * @param signal 信号
     * @param sender 发送者
     * @return
     */ 
    private Envelope createSignalEnvelope(Signal signal, ActorRef sender) {
        return Envelope.builder()
                .message(signal)
                .type(MessageType.SIGNAL)
                .sender(sender != null ? sender : ActorRef.noSender())
                .recipient(getSelfRef())
                .priority(Priority.HIGH)  // 信号消息优先级高
                .build();
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
        return context.getLifecycle().isTerminated();
    }

    @Override
    public ActorContextView getContextView() {
        return new ActorContextView(context);
    }

    /**
     * 发送请求消息
     * @param message 消息
     * @param timeout 超时时间
     * @return
     */
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        return ASK.ask(
                this,
                timeout,
                replyTo -> message
        );
    }

    /**
     * 发送请求消息
     * @param message 消息
     * @return
     */
    public <R> CompletableFuture<R> ask(T message) {
        return ask(message, Duration.ofSeconds(5)); // 默认5秒超时
    }


    @Override
    public boolean preStart() {
       return typedActor.preStart();
    }

    @Override
    public boolean preRestart(Throwable reason) {
       return typedActor.preRestart(reason);
    }

    @Override
    public boolean postRestart(Throwable reason) {
       return typedActor.postRestart(reason);
    }

    @Override
    public boolean preStop() {
       return typedActor.preStop();
    }

    @Override
    public boolean preResume() {
       return typedActor.preResume();
    }

    @Override
    public boolean preSuspend() {
       return ActorLifecycleHook.super.preSuspend();
    }

    // 处理消息的主要方法
    protected void processMessage(Envelope envelope) {
        try {
            // 1. 检查是否需要暂存消息
            if (stashStrategy.shouldStash(envelope)) {
                context.getMailbox().stash(envelope);
                logger.debug("Message stashed: {}", envelope);
                return;
            }

            // 2. 设置消息优先级
            Priority priority = priorityStrategy.getPriority(envelope);
            envelope.setPriority(priority);
            
            // 3. 处理消息
            if (envelope.getMessageType() == MessageType.SIGNAL) {
                handleSignal(envelope);
            } else {
                typedActor.receive(envelope);
            }
            
            // 4. 检查是否可以取出暂存的消息
            if (stashStrategy.shouldUnstash(envelope)) {
                Envelope unstashed = context.getMailbox().unstashOne();
                if (unstashed != null) {
                    logger.debug("Message unstashed: {}", unstashed);
                    processMessage(unstashed); // 递归处理取出的消息
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", envelope, e);
            handleMessageFailure(envelope, e);
        }
    }

    // 处理消息失败的方法
    private void handleMessageFailure(Envelope envelope, Exception e) {
        try {
            if (retryStrategy.shouldRetry(envelope)) {
                handleRetry(envelope, e);
            } else {
                handleFinalFailure(envelope, e);
            }
        } catch (Exception ex) {
            logger.error("Error handling message failure", ex);
            context.getActorSystem().handleSystemFailure(ex, this);
        }
    }

    private void handleRetry(Envelope envelope, Exception e) {
        // 增加重试次数并更新信封
        envelope.incrementRetryCount();
        envelope.setLastError(e);
        
        // 计算延迟时间
        Duration delay = retryStrategy.getRetryDelay(envelope.getRetryCount());
        
        logger.debug("Scheduling retry #{} for message: {} after {}ms", 
            envelope.getRetryCount(), envelope, delay.toMillis());
            
        // 调度重试
        context.getActorSystem().getScheduler().schedule(
            () -> {
                if (!isTerminated()) {
                    context.tell(envelope);
                }
            },
            delay.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    private void handleFinalFailure(Envelope envelope, Exception e) {
        logger.error("Message processing failed after {} retries: {}", 
            envelope.getRetryCount(), envelope);
            
        // 创建失败消息
        ActorFailure failure = new ActorFailure(this, e, envelope);
        
        // 通知监督者
        Envelope failureEnvelope = Envelope.builder()
            .type(MessageType.SYSTEM)
            .message(failure)
            .priority(Priority.HIGH)
            .sender(getSelfRef())
            .recipient(context.getParent().getUnTypedActor())
            .build();
            
        context.getParent().tell(failureEnvelope);
        
        // 向系统报告失败
        context.getActorSystem().handleSystemFailure(e, this);
    }

    // 新增：处理信号消息的辅助方法
    private void handleSignal(Envelope envelope) {
        Signal signal = (Signal) envelope.getMessage();
        switch (signal) {
            case STOP -> preStop();
            case RESTART -> {
                Throwable reason = envelope.getLastError();
                preRestart(reason);
                postRestart(reason);
            }
            case SUSPEND -> preSuspend();
            case RESUME -> preResume();
            default -> logger.warn("Unknown signal received: {}", signal);
        }
    }

    // Getter方法
    public PriorityStrategy getPriorityStrategy() {
        return priorityStrategy;
    }

    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    public StashStrategy getStashStrategy() {
        return stashStrategy;
    }
}
