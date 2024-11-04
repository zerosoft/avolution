package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageHandler;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ActorContext<T> {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);

    private final ActorSystem system;
    private final ActorRef<T> self;
    private final ActorRef<?> parent;
    private final Props<T> props;
    private final AbstractActor<T> actor;
    
    // Actor关系管理
    private final Map<String, ActorRef<?>> children;

    private final Set<ActorRef<?>> watchedActors;

    private final SupervisorStrategy supervisorStrategy;
    
    // 消息处理
    private final Mailbox mailbox;
    private volatile Envelope currentMessage;
    private final AtomicReference<LifecycleState> state;
    
    // 超时控制
    private volatile Duration receiveTimeout;
    private volatile ScheduledFuture<?> receiveTimeoutTask;

    public ActorContext(ActorSystem system, ActorRef<T> self, ActorRef<?> parent, 
                       Props<T> props, AbstractActor<T> actor) {
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.props = props;
        this.actor = actor;
        this.children = new ConcurrentHashMap<>();
        this.watchedActors = ConcurrentHashMap.newKeySet();
        this.mailbox = new Mailbox(100);
        this.state = new AtomicReference<>(LifecycleState.NEW);
        this.supervisorStrategy = props.supervisorStrategy();
        
        initializeActor();
    }

    private void initializeActor() {
        actor.initialize(this);
        if (state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTED)) {
            actor.preStart();
        }
    }

    public void tell(Envelope envelope) {
        if (state.get() == LifecycleState.STARTED) {
            // 将消息放入邮箱
            mailbox.enqueue(envelope);
            // 如果邮箱中只有当前消息，则立即处理
            system.dispatcher().dispatch(self.path(), this::processMailbox);
        }
    }

    private void processMailbox() {
        if (state.get() != LifecycleState.STARTED) {
            return;
        }
        mailbox.process(new MessageHandler() {
            @Override
            public void handle(Envelope currentMessage) throws Exception {
                try {
                    // 记录消息处理时间
                    long startTime = System.nanoTime();

                    actor.onReceive(currentMessage.message());

                    long processingTime = System.nanoTime() - startTime;

                } catch (Exception e) {
                    handleFailure(e, currentMessage);
                } finally {
                    currentMessage = null;
                }
            }
        });

        if (mailbox.hasMessages()) {
            system.dispatcher().dispatch(self.path(), this::processMailbox);
        }
    }

    private void handleFailure(Exception error, Envelope envelope) {
//        SupervisorStrategy.Directive directive = supervisorStrategy.handleException(error);
//        switch (directive) {
//            case RESUME -> logger.warn("Actor resumed after error", error);
//            case RESTART -> restartActor(error);
//            case STOP -> stopActor();
//            case ESCALATE -> escalateFailure(error);
//        }
    }

    public <R> ActorRef<R> spawn(Props<R> props, String name) {
        validateChildName(name);
        String childPath = self.path() + "/" + name;
        ActorRef<R> child = system.actorOf(props, childPath);
        children.put(name, child);
        return child;
    }

    public void watch(ActorRef<?> other) {
        watchedActors.add(other);
        system.deathWatch().watch(self, other);
    }

    public void unwatch(ActorRef<?> other) {
        watchedActors.remove(other);
        system.deathWatch().unwatch(self, other);
    }

    public void setReceiveTimeout(Duration timeout) {
        this.receiveTimeout = timeout;
//        resetReceiveTimeoutTask();
    }

    public ActorRef<T> self() {
        return self;
    }

    public ActorRef<?> parent() {
        return parent;
    }

    public ActorRef<?> sender() {
        return currentMessage != null ? currentMessage.sender() : ActorRef.noSender();
    }

    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    public ActorSystem system() {
        return system;
    }

    private void validateChildName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Child name cannot be null or empty");
        }
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Child with name " + name + " already exists");
        }
    }

    public Map<String, ActorRef<?>> getChildren() {
        return children;
    }
}