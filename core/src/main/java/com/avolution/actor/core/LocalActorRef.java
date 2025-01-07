package com.avolution.actor.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.core.context.ActorContextView;
import com.avolution.actor.message.Signal;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;

/**
 * 本地Actor引用
 * 代表本地JVM中的Actor实例
 */
public class LocalActorRef<T> implements ActorRef<T> {
    private final UnTypedActor<T> unTypedActor;
    private final String path;
    private final String name;
    private final ActorRef<IDeadLetterActorMessage> deadLetters;

    public LocalActorRef(UnTypedActor<T> unTypedActor, String path, String name, ActorRef<IDeadLetterActorMessage> deadLetters) {
        this.unTypedActor = unTypedActor;
        this.path = path;
        this.name = name;
        this.deadLetters = deadLetters;
    }

    @Override
    public void tell(T message, ActorRef sender) {
        unTypedActor.tell(message, sender);
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        unTypedActor.tell(signal, sender);
    }

    @Override
    public <R> CompletableFuture<R> ask(T message, Duration timeout) {
        return unTypedActor.ask(message, timeout);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isTerminated() {
        return unTypedActor.isTerminated();
    }

    @Override
    public ActorContext getContext() {
        return unTypedActor.getContext();
    }

    @Override
    public ActorContextView getContextView() {
        return new ActorContextView(getContext());
    }
}
