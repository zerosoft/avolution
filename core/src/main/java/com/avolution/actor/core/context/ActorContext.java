package com.avolution.actor.core.context;

import com.avolution.actor.core.*;
import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorStopException;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.*;
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
public class ActorContext  {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);
    // Actor路径
    private final String path;
    // Actor系统
    private final ActorSystem system;
    // Actor实例,强绑定
    private final AbstractActor<?> self;

    private final ActorContext parent;

    // Actor关系管理
    private final Map<String, AbstractActor> children;

    private SupervisorStrategy supervisorStrategy;
    
    // 消息处理
    protected final Mailbox mailbox;

    private final AtomicReference<LifecycleState> state = new AtomicReference<>(LifecycleState.NEW);

    private final ActorScheduler scheduler;
    // 添加监视器管理
    private final Map<ActorRef<?>, Set<Runnable>> watchCallbacks = new ConcurrentHashMap<>();

    public ActorContext(String path, ActorSystem system, AbstractActor<?> self, ActorContext parent,
                        Props<?> props) {
        this.path = path;
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.children = new ConcurrentHashMap<>();
        this.mailbox = new Mailbox(100);
        this.supervisorStrategy = props.supervisorStrategy();
        this.scheduler=new DefaultActorScheduler();
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

    public void tell(Envelope envelope) {
        if (state.get() == LifecycleState.RUNNING) {
            // 将消息放入邮箱
            mailbox.enqueue(envelope);
            // 如果邮箱中只有当前消息，则立即处理
            system.dispatcher().dispatch(path, this::processMailbox);
        }
    }

    private void processMailbox() {
        if (state.get() != LifecycleState.RUNNING) {
            return;
        }
        mailbox.process(self);

        if (mailbox.hasMessages()) {
            system.dispatcher().dispatch(path, this::processMailbox);
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

        // 使用当前Actor的消息处理线程
        self.tell(new StopMessage(stopFuture), self.getSelf());

        return stopFuture;
    }

    public void handleStop(StopMessage message) {
        if (state.get() == LifecycleState.STOPPED) {
            message.future.complete(null);
            return;
        }

        try {
            // 1. 暂停邮箱
            mailbox.suspend();
            state.set(LifecycleState.STOPPING);

            // 2. 停止子Actor
            if (!children.isEmpty()) {
                // 创建子Actor停止的Future列表
                List<CompletableFuture<Void>> childStopFutures = new ArrayList<>();
                Map<String, AbstractActor> childrenSnapshot = new HashMap<>(children);

                // 停止所有子Actor
                childrenSnapshot.forEach((childPath, child) -> {

                    CompletableFuture<Void> childFuture = new CompletableFuture<>();
                    try {
                        // 暂停子Actor邮箱
                        child.getContext().suspend();
                        // 发送停止消息
                        child.stopByParent().orTimeout(2, TimeUnit.SECONDS).whenComplete((v, ex) -> {
                                    if (ex != null) {
                                        logger.warn("Child actor stop timeout: {}, forcing stop...", childPath);
                                        forceStopChild(child, childPath);
                                    }
                                    childFuture.complete(null);
                                });
                    } catch (Exception e) {
                        logger.error("Error stopping child actor: {}", childPath, e);
                        forceStopChild(child, childPath);
                        childFuture.complete(null);
                    }

                    childStopFutures.add(childFuture);
                });

                // 等待所有子Actor停止
                CompletableFuture.allOf(childStopFutures.toArray(new CompletableFuture[0]))
                        .orTimeout(2, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            logger.warn("Timeout waiting for children to stop, forcing stop remaining children");
                            forceStopRemainingChildren();
                            return null;
                        })
                        .join();
            }


            // 3. 停止自身
            self.stop()
                    .orTimeout(1, TimeUnit.SECONDS)
                    .whenComplete((v, e) -> {
                        if (e != null) {
                            self.forceStop();
                        }

                        // 4. 清理资源
                        cleanupContextResources();

                        // 5. 设置状态
                        state.set(LifecycleState.STOPPED);
                        message.future.complete(null);
                    });

        } catch (Exception e) {
            logger.error("Error during actor stop: {}", path, e);
            state.set(LifecycleState.STOPPED);
            message.future.completeExceptionally(e);
        }
    }


    private void forceStopChild(AbstractActor child, String childPath) {
        if (child == null || !children.containsKey(childPath)) {
            children.remove(childPath);
            return;
        }

        try {
            // 1. 暂停子Actor的邮箱
            child.getContext().suspend();

            // 3.  中断当前消息处理 强制停止子Actor
            child.forceStop();

            // 4. 清理资源
            cleanupChildResources(child, childPath);

            // 5. 通知监视者
            notifyWatchers(child.getSelf());

            logger.debug("Force stopped child actor: {}", childPath);

        } catch (Exception e) {
            logger.error("Error during force stop of child: {}", childPath, e);
        } finally {
            // 6. 从children中移除并清理上下文
            children.remove(childPath);
            child.getContext().setState(LifecycleState.STOPPED);
            child.setContext(null);
        }
    }



    /**
     * 并行强制停止所有子Actor
     */
    private void forceStopRemainingChildren() {
        if (children.isEmpty()) {
            return;
        }

        // 创建快照避免并发修改
        Map<String, AbstractActor> remainingChildren = new HashMap<>(children);

        // 并行强制停止所有子Actor
        CompletableFuture.runAsync(() -> {
                    remainingChildren.forEach((childPath, child) -> {
                        try {
                            forceStopChild(child, childPath);
                        } catch (Exception e) {
                            logger.error("Failed to force stop child: {}", childPath, e);
                        }
                    });
                }).orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Timeout while force stopping remaining children");
                    // 确保清理所有引用
                    children.clear();
                    return null;
                });
    }

    private void cleanupChildResources(AbstractActor child, String childPath) {
        try {
            // 1. 从注册表中注销
            system.unregisterActor(child.path());

            // 2. 清理监视关系
            unwatch(child.getSelf());

        } catch (Exception e) {
            logger.error("Error cleaning up resources for child: {}", childPath, e);
        }
    }

    public void cleanupContextResources() {
        // 关闭调度器
        try {
            scheduler.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error shutting down scheduler", e);
        }

        // 从系统注销
        system.unregisterActor(path);

        // 清理监视关系
        unwatch(self.getSelf());

        // 清理子Actor引用
        children.clear();

        mailbox.close();
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
        if (parent != null) {
            SupervisionMessage supervisionMessage = new SupervisionMessage(self.getSelf(), cause);
            Envelope envelope=new Envelope(supervisionMessage,self.getSelf(),parent.self.getSelf(), MessageType.NORMAL,1);
            parent.tell(envelope);
        } else {
            logger.error("No parent to escalate error to, stopping self", cause);
            stop(self.getSelf());
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
