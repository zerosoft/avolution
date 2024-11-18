package com.avolution.actor.core.context;

import com.avolution.actor.core.*;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorStopException;
import com.avolution.actor.exception.MailboxException;
import com.avolution.actor.exception.SystemFailureException;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.*;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.lifecycle.LifecycleState;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Actor上下文
 */
public class ActorContext  {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);

    // ====== 核心字段 ======
    private final String path;                    // Actor路径
    private final ActorSystem system;             // Actor系统引用
    private final AbstractActor<?> self;          // Actor实例
    private final ActorContext parent;            // 父Actor上下文
    private final Map<String, AbstractActor> children = new ConcurrentHashMap<>();  // 子Actor映射
    private SupervisorStrategy supervisorStrategy; // 监督策略
    private final Mailbox mailbox;                // 消息邮箱
    private final AtomicReference<LifecycleState> state = new AtomicReference<>(LifecycleState.NEW);
    private final ActorScheduler scheduler;        // 调度器
    private final Map<ActorRef<?>, Set<Runnable>> watchCallbacks = new ConcurrentHashMap<>(); // 监视回调

    /**
     * 初始化Actor上下文
     * @param path Actor路径
     * @param system Actor系统
     * @param self Actor实例
     * @param parent 父Actor上下文
     * @param props Actor属性配置
     */
    public ActorContext(String path, ActorSystem system, AbstractActor<?> self,
                        ActorContext parent, Props props) {
        this.path = path;
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.mailbox = new Mailbox(system, props.throughput());
        this.supervisorStrategy = props.supervisorStrategy();
        this.scheduler = new DefaultActorScheduler();
    }


    public void initializeActor() {
        if (state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTING)) {
            try {
                self.initialize();

                self.start();

                state.set(LifecycleState.RUNNING);
            } catch (Exception e) {
                state.set(LifecycleState.STOPPED);
                throw new ActorInitializationException("Failed to initialize actor", e);
            }
        }
    }

    public void tell(Envelope<?> envelope) {
        if (state.get() == LifecycleState.RUNNING) {
            mailbox.enqueue(envelope);
            if (mailbox.hasMessages()) {
                system.dispatcher().dispatch(path, this::processMailbox);
            }
        } else {
            handleDeadLetter(envelope);
        }
    }

    private void handleDeadLetter(Envelope<?> envelope) {
        IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                envelope.getMessage(),
                envelope.getSender().path(),
                envelope.getRecipient().path(),
                LocalDateTime.now()+"",
                envelope.getMessageType(),
                envelope.getRetryCount(),new HashMap<>()
        );

        // 记录死信
        logger.warn("Dead letter received: {}", deadLetter);

        // 发送到系统的死信Actor
        system.getDeadLetters().tell(deadLetter, self.getSelf());
    }

    /**
     * 处理邮箱中的消息
     * 确保消息按顺序处理，并在必要时重新调度
     */
    private void processMailbox() {
        if (state.get() != LifecycleState.RUNNING) {
            return;
        }

        try {
            mailbox.process(self);
            if (mailbox.hasMessages()) {
                system.dispatcher().dispatch(path, this::processMailbox);
            }
        } catch (Exception e) {
            logger.error("Error processing mailbox for actor: {}", path, e);
            escalate(e);
        }
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

    /**
     * 获取子Actor的映射
     * @return
     */
    public Map<String, ActorRef> getChildren() {
        Map<String, ActorRef> result=new HashMap<>();
        Set<String> keySets = children.keySet();
        for (String keySet : keySets) {
            result.put(keySet,children.get(keySet).getSelf());
        }
        return result;
    }

    public void addChild(String name, AbstractActor actor) {
        children.put(name, actor);
    }

    public String getPath() {
        return path;
    }

    private void setState(LifecycleState lifecycleState) {
        this.state.set(lifecycleState);
    }

    /**
     * 创建启动Actor
     */
    public void start() {
        if (state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTING)) {
            try {
                self.start();
                state.set(LifecycleState.RUNNING);
            } catch (Exception e) {
                state.set(LifecycleState.STOPPED);
                throw e;
            }
        }
    }

    /**
     * 关闭Actor
     * @return 返回停止的Future
     */
    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        if (state.get() == LifecycleState.STOPPED) {
            stopFuture.complete(null);
            return stopFuture;
        }

        try {
            // 1. 暂停邮箱
            mailbox.suspend();
            state.set(LifecycleState.STOPPING);

            // 2. 停止子Actor
            List<CompletableFuture<Void>> childStopFutures = stopChildren();

            // 3. 等待所有子Actor停止
            CompletableFuture.allOf(childStopFutures.toArray(new CompletableFuture[0]))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Error stopping children of actor: {}", path, throwable);
                        }
                        // 4. 完成停止过程
                        completeStop(stopFuture);
                    });

        } catch (Exception e) {
            logger.error("Error initiating stop for actor: {}", path, e);
            stopFuture.completeExceptionally(e);
        }

        return stopFuture;
    }

    /**
     * 停止所有子Actor
     * 确保按照正确的顺序停止，并处理超时和错误情况
     * @return 返回所有子Actor的停止Future列表
     */
    private List<CompletableFuture<Void>> stopChildren() {
        List<CompletableFuture<Void>> childStopFutures = new ArrayList<>();
        Map<String, AbstractActor> childrenSnapshot = new HashMap<>(children);

        childrenSnapshot.forEach((childName, child) -> {
            try {
                CompletableFuture<Void> childFuture = child.stopByParent()
                        .orTimeout(5, TimeUnit.SECONDS)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                if (throwable instanceof TimeoutException) {
                                    logger.warn("Timeout stopping child actor: {}, forcing stop", child.path());
                                    child.forceStop();
                                } else {
                                    logger.error("Error stopping child actor: {}", child.path(), throwable);
                                }
                            }
                            // 移除引用关系但不重置context
                            children.remove(childName);
                        });

                childStopFutures.add(childFuture);

            } catch (Exception e) {
                logger.error("Error initiating stop for child actor: {}", child.path(), e);
                child.forceStop();
                children.remove(childName);
            }
        });

        return childStopFutures;
    }

    private void completeStop(CompletableFuture<Void> stopFuture) {
        try {
            // 1. 清理资源
            children.clear();
            scheduler.shutdown();
            mailbox.clearMailbox();

            // 2. 更新状态
            state.set(LifecycleState.STOPPED);

            // 3. 通知观察者
//            notifyWatchers(watchCallbacks.keySet().t);

            stopFuture.complete(null);
        } catch (Exception e) {
            logger.error("Error completing stop for actor: {}", path, e);
            stopFuture.completeExceptionally(e);
        } catch (MailboxException e) {
            throw new RuntimeException(e);
        }
    }

    // 添加重启逻辑
    public void restart(Throwable reason) {
        if (state.compareAndSet(LifecycleState.RUNNING, LifecycleState.RESTARTING)) {
            try {
                // 1. 执行重启前回调
                self.onPreRestart(reason);

                // 2. 停止当前Actor
                CompletableFuture<Void> stopFuture = stop();
                stopFuture.get(5, TimeUnit.SECONDS);

                // 3. 重新初始化
                initializeActor();

                // 4. 执行重启后回调
                self.onPostRestart(reason);

                state.set(LifecycleState.RUNNING);
            } catch (Exception e) {
                state.set(LifecycleState.STOPPED);
                throw new ActorInitializationException("Failed to restart actor", e);
            }
        }
    }

    public void watch(ActorRef<?> target, Runnable callback) {
        if (target == null || callback == null) {
            return;
        }

        watchCallbacks.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet())
                .add(callback);

        // 如果目标Actor已经终止，立即触发回调
        if (!(!(target instanceof AbstractActor) ||
                !target.isTerminated())) {
            callback.run();
            unwatch(target);
        }
    }

    public void unwatch(ActorRef<?> target) {
        if (target != null) {
            watchCallbacks.remove(target);
        }
    }

    // 通知监视器目标Actor已终止
    public void notifyWatchers(ActorRef<?> target) {
        Set<Runnable> callbacks = watchCallbacks.remove(target);
        if (callbacks != null) {
            callbacks.forEach(callback -> {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.error("Error executing watch callback for {}", target.path(), e);
                }
            });
        }
    }

    public void stop(ActorRef actorRef) {
        if (actorRef == null) {
            throw new IllegalArgumentException("ActorRef cannot be null");
        }

        String actorPath = actorRef.path();
        String actorName = actorRef.name();

        try {
            // 检查是否为子Actor
            if (!children.containsKey(actorName)) {
                logger.warn("Attempting to stop non-child actor: {}", actorPath);
                return;
            }

            AbstractActor child = children.get(actorName);

            // 1. 暂停子Actor的邮箱
            child.getContext().suspend();

            // 2. 发送停止消息
            CompletableFuture<Void> stopFuture = child.stop();

            // 3. 等待停止完成或超时
            try {
                stopFuture.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Timeout waiting for actor to stop: {}, forcing stop", actorPath);
                child.forceStop();
            }

            // 4. 清理资源
            children.remove(actorName);
            child.setContext(null);

            // 5. 通知监视者
            notifyWatchers(actorRef);

            logger.debug("Successfully stopped actor: {}", actorPath);
        } catch (Exception e) {
            logger.error("Error while stopping actor: {}", actorPath, e);
            // 确保即使发生错误也能移除Actor
            children.remove(actorName);
            throw new ActorStopException("Failed to stop actor: " + actorPath, e);
        }
    }

    public ActorContext getParent() {
        return parent;
    }

    public <R> ActorRef<R> actorOf(Props<R> props, String name) {
        validateChildName(name);
        ActorRef<R> child = system.actorOf(props, name,this);
        return child;
    }

    // 添加定时器功能
    public <T> void scheduleOnce(String key, Duration delay, T message) {
        scheduler.scheduleOnce(key, delay, message,
                msg -> tell(new Envelope(msg, ActorRef.noSender(), self.getSelf(), MessageType.NORMAL, 1)));
    }

    public void scheduleRepeatedly(String key, Duration initialDelay, Duration interval, Object message) {
        scheduler.scheduleRepeatedly(key, initialDelay, interval, message,
                msg -> tell(new Envelope(msg, ActorRef.noSender(), self.getSelf(), MessageType.NORMAL, 1)));
    }


    public void resume() {
        if (self.getSelf() instanceof AbstractActor actor) {
            mailbox.resume();
        }
    }

    public void escalate(Throwable cause) {
        try {
            // 1. 暂停当前 actor 的邮箱
            mailbox.suspend();

            // 2. 获取父 actor 上下文
            ActorContext parentContext = getParent();
            if (parentContext == null) {
                // 如果没有父 actor，则交给系统处理
                system.handleSystemFailure(cause, self.getSelf());
                return;
            }

            // 3. 创建升级信号
            SignalEnvelope escalateSignal = new SignalEnvelope(
                    Signal.ESCALATE,
                    self.getSelf(),
                    (ActorRef<Signal>) parentContext.getSelf(),
                    SignalPriority.HIGH,
                    SignalScope.SINGLE
            );

            // 4. 添加错误信息到信号的附加数据
            escalateSignal.addAttachment("cause", cause);
            escalateSignal.addAttachment("child", self.getSelf());

            // 5. 发送升级信号给父 actor
            parentContext.getSelf().tell(escalateSignal,getSelf());

        } catch (Exception e) {
            logger.error("Error during failure escalation for actor: {}", path, e);
            // 如果升级过程失败，强制停止当前 actor
            if (self.getSelf() instanceof AbstractActor actor) {
                actor.forceStop();
            }
        }
    }

    public void setSupervisorStrategy(SupervisorStrategy supervisorStrategy) {
        this.supervisorStrategy=supervisorStrategy;
    }

    public ActorRef getSelf() {
        return self.getSelf();
    }

    public AtomicReference<LifecycleState> getState() {
        return state;
    }

    public IScheduler getScheduler() {
        return scheduler;
    }

    public void suspend() {
        mailbox.suspend();
    }

    public Mailbox getMailbox() {
        return mailbox;
    }


}
