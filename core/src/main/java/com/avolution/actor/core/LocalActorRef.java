package com.avolution.actor.core;

import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;
import com.avolution.actor.message.Terminated;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 当前JVM的Actor引用
 * @param <T>
 */
public class LocalActorRef<T> implements ActorRef<T> {
    // 弱引用，避免循环引用
    private WeakReference<AbstractActor<T>> actor;
    // 原始路径
    private final String originalPath;
    // 原始名称
    private final String originalName;
    // 死信Actor
    private ActorRef<IDeadLetterActorMessage> deadLetters;

    public LocalActorRef(AbstractActor<T> actor,String originalPath,String originalName,ActorRef<IDeadLetterActorMessage> deadLetters) {
        this.actor = new WeakReference<>(actor);
        this.originalPath =originalPath;
        this.originalName = originalName;
        this.deadLetters =deadLetters;
    }

    private void handleDeadLetter(Object message, ActorRef sender) {
        if (deadLetters == null) {
            return;
        }
        IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                message,
                sender.path(),
                originalPath,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                MessageType.NORMAL.toString(),
                0
        );
        deadLetters.tell(deadLetter, ActorRef.noSender());
    }

    @Override
    public void tell(T message, ActorRef sender) {
        if (isTerminated()) {
            handleDeadLetter(message, sender);
        } else {
            AbstractActor<T> actorInstance = actor.get();
            if (actorInstance != null && !actorInstance.isTerminated()) {
                actorInstance.tell(message, sender);
            } else {
                handleDeadLetter(message, sender);
            }
        }
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (isTerminated()) {
            handleDeadLetter(signal, sender);
        } else {
            AbstractActor<T> actorInstance = actor.get();
            if (actorInstance != null && !actorInstance.isTerminated()) {
                actorInstance.tell(signal, sender);
            } else {
                handleDeadLetter(signal, sender);
            }
        }
    }

    @Override
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        if (isTerminated()) {
            CompletableFuture<R> future = new CompletableFuture<>();
            handleDeadLetter(message, ActorRef.noSender());
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
