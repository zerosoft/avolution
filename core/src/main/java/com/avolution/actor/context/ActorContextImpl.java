package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.supervision.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ActorContextImpl<T> implements ActorContext<T> {
    private static final Logger log = LoggerFactory.getLogger(ActorContextImpl.class);

    // 核心组件
    private final ActorSystem system;
    private final ActorRef<T> self;
    private final ActorRef<?> parent;
    private final Props<T> props;
    private final SupervisorStrategy supervisorStrategy;

    // Actor状态管理
    private final AtomicReference<ActorState> state;
    private final Map<String, ActorRef<?>> children;
    private final Set<ActorRef<?>> watchedActors;
    private volatile Duration receiveTimeout;
    private ScheduledFuture<?> receiveTimeoutTask;
    private Envelope currentMessage;

    public ActorContextImpl(ActorSystem system, ActorRef<T> self, ActorRef<?> parent, Props<T> props) {
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.props = props;
        this.supervisorStrategy = props.supervisorStrategy();
        
        this.state = new AtomicReference<>(ActorState.NEW);
        this.children = new ConcurrentHashMap<>();
        this.watchedActors = ConcurrentHashMap.newKeySet();
    }

    @Override
    public ActorRef<T> self() {
        return self;
    }

    @Override
    public ActorRef<?> sender() {
        return currentMessage != null ? currentMessage.getSender() : ActorRef.noSender();
    }

    @Override
    public ActorSystem system() {
        return system;
    }

    @Override
    public ActorRef<T> spawn(Props<T> props, String name) {
        validateChildName(name);
        String path = self.path() + "/" + name;
        ActorRef<T> child = system.actorOf(props, path);
        children.put(name, child);
        return child;
    }

    @Override
    public void watch(ActorRef<?> other) {
        watchedActors.add(other);
        system.deathWatch().watch(self, other);
    }

    @Override
    public void unwatch(ActorRef<?> other) {
        watchedActors.remove(other);
        system.deathWatch().unwatch(self, other);
    }

    @Override
    public void stop(ActorRef<?> child) {
        if (children.containsValue(child)) {
            child.tell(PoisonPill.INSTANCE, self);
            children.values().remove(child);
        }
    }

    @Override
    public void setReceiveTimeout(Duration timeout) {
        this.receiveTimeout = timeout;
        resetReceiveTimeoutTask();
    }

    @Override
    public ActorRef<?> parent() {
        return parent;
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    // 内部方法
    void setCurrentMessage(Envelope message) {
        this.currentMessage = message;
    }

    private void resetReceiveTimeoutTask() {
        if (receiveTimeoutTask != null) {
            receiveTimeoutTask.cancel(true);
        }
        if (receiveTimeout != null) {
            receiveTimeoutTask = system.scheduler().schedule(
                () -> self.tell(new ReceiveTimeout(), ActorRef.noSender()),
                receiveTimeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }
    }

    private void validateChildName(String name) {
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Child with name '" + name + "' already exists");
        }
    }

    private enum ActorState {
        NEW, STARTING, STARTED, STOPPING, STOPPED
    }

    private static class ReceiveTimeout {}
}