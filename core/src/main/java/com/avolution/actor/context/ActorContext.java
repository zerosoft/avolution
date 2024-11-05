package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.exception.ActorCreationException;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Actor上下文
 */
public class ActorContext {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);

    private final String path;
    private final ActorSystem system;
    private final AbstractActor<?> self;
    private final ActorRef<?> parent;

    // Actor关系管理
    private final Map<String, ActorRef> children;

    private final Set<ActorRef<?>> watchedActors;

    private final SupervisorStrategy supervisorStrategy;
    
    // 消息处理
    private final Mailbox mailbox;

    private final AtomicReference<LifecycleState> state;
    
    // 超时控制
    private volatile Duration receiveTimeout;
    private volatile ScheduledFuture<?> receiveTimeoutTask;

    public ActorContext(String path, ActorSystem system, AbstractActor<?> self, ActorRef<?> parent,
                        Props<?> props) {
        this.path=path;
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.children = new ConcurrentHashMap<>();
        this.watchedActors = ConcurrentHashMap.newKeySet();
        this.mailbox = new Mailbox(100);
        this.state = new AtomicReference<>(LifecycleState.NEW);
        this.supervisorStrategy = props.supervisorStrategy();
        // 初始化Actor
        initializeActor();
    }

    /**
     * 初始化Actor
     */
    private void initializeActor() {
        self.initialize(this);
        if (state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTED)) {
            self.preStart();
        }
    }

    /**
     * 发送消息给Actor
     * @param envelope 消息封装
     */
    public void tell(Envelope envelope) {
        if (state.get() == LifecycleState.STARTED) {
            // 将消息放入邮箱
            mailbox.enqueue(envelope);
            // 如果邮箱中只有当前消息，则立即处理
            system.dispatcher().dispatch(self.path(), this::processMailbox);
        }
    }

    /**
     * 处理邮箱中的消息
     */
    private void processMailbox() {
        if (state.get() != LifecycleState.STARTED) {
            return;
        }
        mailbox.process(self);

        if (mailbox.hasMessages()) {
            system.dispatcher().dispatch(self.path(), this::processMailbox);
        }
    }

    /**
     * 处理失败
     * @param error 异常
     * @param envelope 消息封装
     */
    public void handleFailure(Exception error, Envelope envelope) {
        Directive directive = supervisorStrategy.handle(error);
        switch (directive) {
            case RESUME -> logger.warn("Actor resumed after error", error);
            case RESTART -> self.preRestart(error);
            case STOP -> self.postStop();
            case ESCALATE -> self.postRestart(error);
        }
    }

    /**
     * 监视其他Actor
     * @param other 其他Actor
     */
    public void watch(ActorRef<?> other) {
        watchedActors.add(other);
        system.deathWatch().watch(self, other);
    }

    /**
     * 取消监视其他Actor
     * @param other 其他Actor
     */
    public void unwatch(ActorRef<?> other) {
        watchedActors.remove(other);
        system.deathWatch().unwatch(self, other);
    }

    /**
     * 设置接收超时
     * @param timeout 超时时间
     */
    public void setReceiveTimeout(Duration timeout) {
        this.receiveTimeout = timeout;
    }

    /**
     * 获取监督策略
     * @return 监督策略
     */
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    /**
     * 获取Actor系统
     * @return Actor系统
     */
    public ActorSystem system() {
        return system;
    }

    /**
     * 验证子Actor名称
     * @param name 子Actor名称
     */
    private void validateChildName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Child name cannot be null or empty");
        }
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Child with name " + name + " already exists");
        }
    }

    /**
     * 获取子Actor
     * @return 子Actor
     */
    public Map<String, ActorRef> getChildren() {
        return children;
    }

    /**
     * 获取Actor路径
     * @return Actor路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 停止Actor
     */
    public void stop() {
        if (state.compareAndSet(LifecycleState.STARTED, LifecycleState.STOPPING)) {
            self.postStop();
            state.set(LifecycleState.STOPPED);
        }
    }

    /**
     * 创建子Actor
     * @param props 子Actor属性
     * @param name 子Actor名称
     * @param <R> 子Actor类型
     * @return 子Actor引用
     */
    public <R> ActorRef<R> actorOf(Props<R> props, String name) {
        validateChildName(name);
        String childPath = self.path() + "/" + name;
        ActorRef<R> child = system.actorOf(props, childPath, self);
        children.put(name, child);
        return child;
    }
}
