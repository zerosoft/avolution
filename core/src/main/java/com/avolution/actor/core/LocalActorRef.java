package com.avolution.actor.core;

import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 当前JVM的Actor引用
 * @param <T>
 */
public class LocalActorRef<T> implements ActorRef<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalActorRef.class);
    // 弱引用，避免循环引用
    private WeakReference<AbstractActor<T>> actor;
    // 原始路径
    private final String originalPath;
    // 原始名称
    private final String originalName;
    // 死信Actor
    private ActorRef<IDeadLetterActorMessage> deadLetters;

    private final AtomicInteger retryCount;
    private final Duration retryTimeout;

    public LocalActorRef(AbstractActor<T> actor,String originalPath,String originalName,ActorRef<IDeadLetterActorMessage> deadLetters) {
        this.actor = new WeakReference<>(actor);
        this.originalPath =originalPath;
        this.originalName = originalName;
        this.deadLetters =deadLetters;
        this.retryCount = new AtomicInteger(0);
        this.retryTimeout = Duration.ofSeconds(5);
    }

    private void handleDeadLetter(Object message, ActorRef sender, MessageType messageType) {
        if (deadLetters == null) {
            LOGGER.warn("Dead letter actor not available, message dropped: {}", message);
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("failureReason", "Actor terminated or unavailable");
        metadata.put("originalSender", sender.path());
        metadata.put("retryCount", retryCount.get());
        metadata.put("timestamp", System.currentTimeMillis());

        IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                message,
                sender.path(),
                originalPath,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                messageType,
                retryCount.get(),
                metadata
        );

        deadLetters.tell(deadLetter, ActorRef.noSender());
        LOGGER.warn("Message sent to dead letters: {}", deadLetter);
    }

    @Override
    public void tell(T message, ActorRef sender) {
        if (isTerminated()) {
            handleDeadLetter(message, sender, MessageType.NORMAL);
            return;
        }

        AbstractActor<T> actorInstance = actor.get();
        if (actorInstance != null && !actorInstance.isTerminated()) {
            try {
                actorInstance.tell(message, sender);
                retryCount.set(0); // 重置重试计数
            } catch (Exception e) {
                LOGGER.error("Error telling message to actor: {}", originalPath, e);
                if (retryCount.incrementAndGet() <= 3) {
                    retryMessage(message, sender);
                } else {
                    handleDeadLetter(message, sender, MessageType.NORMAL);
                }
            }
        } else {
            handleDeadLetter(message, sender, MessageType.NORMAL);
        }
    }

    private void retryMessage(T message, ActorRef sender) {
        CompletableFuture.delayedExecutor(
                        retryTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> tell(message, sender));
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (isTerminated()) {
            handleDeadLetter(signal, sender, MessageType.SIGNAL);
            return;
        }

        AbstractActor<T> actorInstance = actor.get();
        if (actorInstance != null && !actorInstance.isTerminated()) {
            try {
                actorInstance.tell(signal, sender);
            } catch (Exception e) {
                LOGGER.error("Error telling signal to actor: {}", originalPath, e);
                handleDeadLetter(signal, sender, MessageType.SIGNAL);
            }
        } else {
            handleDeadLetter(signal, sender, MessageType.SIGNAL);
        }
    }

    @Override
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        if (isTerminated()) {
            CompletableFuture<R> future = new CompletableFuture<>();
            handleDeadLetter(message, ActorRef.noSender(), MessageType.NORMAL);
            future.completeExceptionally(new IllegalStateException("ActorRef is no longer valid"));
            return future;
        }
        return Objects.requireNonNull(actor.get()).ask(message, timeout);
    }

    @Override
    public String path() {
        return originalPath;
    }

    @Override
    public String name() {
        return originalName;
    }

    @Override
    public boolean isTerminated() {
        return actor == null || actor.get() == null || actor.get().isTerminated();
    }

}
