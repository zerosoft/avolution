package com.avolution.actor.core;

import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;
import com.avolution.actor.message.Terminated;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class ActorRefProxy<T> implements ActorRef<T> {
    private AbstractActor<T> actor;
    private volatile boolean isValid = true;
    private final String originalPath;
    private final String originalName;

    public ActorRefProxy(AbstractActor<T> actor) {
        this.actor = actor;
        this.originalPath = actor.path();
        this.originalName = actor.name();
        actor.setSelfRef(this);
    }

    private void handleDeadLetter(Object message, ActorRef sender) {
        if (actor != null && actor.context != null && actor.context.system() != null) {
            IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                    message,
                    sender.path(),
                    originalPath,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    MessageType.NORMAL.toString(),
                    0
            );

            actor.context.system().getDeadLetters().tell(deadLetter, sender);
        }
    }

    @Override
    public void tell(T message, ActorRef sender) {
        if (!isValid || actor == null) {
            handleDeadLetter(message, sender);
            return;
        }
        actor.tell(message, sender);
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        if (!isValid || actor == null) {
            handleDeadLetter(signal, sender);
            return;
        }
        actor.tell(signal, sender);
    }

    @Override
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        if (!isValid || actor == null) {
            CompletableFuture<R> future = new CompletableFuture<>();
            handleDeadLetter(message, ActorRef.noSender());
            future.completeExceptionally(new IllegalStateException("ActorRef is no longer valid"));
            return future;
        }
        return actor.ask(message, timeout);
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
        return !isValid || actor == null || actor.isTerminated();
    }

    void invalidate() {
        if (isValid && actor != null) {
            handleDeadLetter(new Terminated(), ActorRef.noSender());
        }
        isValid = false;
        actor = null;
    }

    public void destroy() {
        actor=null;
    }

}
