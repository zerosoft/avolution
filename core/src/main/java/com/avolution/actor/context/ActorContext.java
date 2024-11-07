package com.avolution.actor.context;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import com.avolution.actor.core.*;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.ReceiveTimeout;
import com.avolution.actor.message.Signal;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Actor上下文
 */
public class ActorContext {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);

    private final String path;
    private final ActorSystem system;

    private final WeakReference<AbstractActor<?>> self;

    private final ActorContext parent;

    private final AtomicInteger id = new AtomicInteger(0);

    private boolean isRoot(){
        return parent == this;
    }

    // Actor关系管理
    private final Map<String, ActorRef> children;

    private final Set<ActorRef<?>> watchedActors;

    private final SupervisorStrategy supervisorStrategy;
    
    // 消息处理
    private final Mailbox mailbox;

    private final AtomicReference<LifecycleState> state;

    private final ScheduledExecutorService scheduler;
    // 超时控制
    private volatile Duration receiveTimeout;
    private volatile ScheduledFuture<?> receiveTimeoutTask;

    // 添加新的计时器管理
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public ActorContext(String path, ActorSystem system, AbstractActor<?> self, ActorContext parent,
                        Props<?> props) {
        this.path = path + "/" + generateUniqueId();
        this.system = system;
        this.self = new WeakReference<>(self);
        this.parent = parent;
        this.children = new ConcurrentHashMap<>();
        this.watchedActors = ConcurrentHashMap.newKeySet();
        this.mailbox = new Mailbox(100);
        this.state = new AtomicReference<>(LifecycleState.NEW);
        this.supervisorStrategy = props.supervisorStrategy();
        this.scheduler=new VirtualThreadScheduler();
        // 初始化Actor
        initializeActor();
    }

    private int generateUniqueId() {
        return id.incrementAndGet();
    }

    private void initializeActor() {
        AbstractActor<?> actor = self.get();
        if (actor != null) {
            actor.initialize(this);
            if (state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTED)) {
                actor.preStart();
            }
        }
    }

    public void tell(Envelope envelope) {
        if (state.get() == LifecycleState.STARTED) {
            // 将消息放入邮箱
            mailbox.enqueue(envelope);
            // 如果邮箱中只有当前消息，则立即处理
            system.dispatcher().dispatch(self.get().path(), this::processMailbox);
        }
    }

    private void processMailbox() {
        if (state.get() != LifecycleState.STARTED) {
            return;
        }
        mailbox.process(self.get());

        if (mailbox.hasMessages()) {
            system.dispatcher().dispatch(self.get().path(), this::processMailbox);
        }
    }

    // 添加消息处理失败的处理逻辑
    public void handleFailure(Exception error, Envelope envelope) {
        Directive directive = supervisorStrategy.handle(error);
        switch (directive) {
            case RESUME:
                logger.warn("Actor resumed after error", error);
                break;
            case RESTART:
                logger.warn("Actor restarting after error", error);
                restart(error);
                break;
            case STOP:
                logger.warn("Actor stopping after error", error);
                stop();
                break;
            case ESCALATE:
                if (parent != null) {
                    parent.handleFailure(error, envelope);
                } else {
                    logger.error("Error escalated to root actor, stopping actor system", error);
                    system.terminate();
                }
                break;
        }
    }


    public void watch(ActorRef<?> other) {
        watchedActors.add(other);
        system.deathWatch().watch(self.get(), other);
    }

    public void unwatch(ActorRef<?> other) {
        watchedActors.remove(other);
        system.deathWatch().unwatch(self.get(), other);
    }

    public void setReceiveTimeout(Duration timeout) {
        if (receiveTimeoutTask != null) {
            receiveTimeoutTask.cancel(false);
        }

        if (timeout != null && !timeout.isZero()) {
            this.receiveTimeout = timeout;
            this.receiveTimeoutTask = scheduler.schedule(() -> {
                if (!mailbox.hasMessages()) {
                    self.get().tell(ReceiveTimeout.INSTANCE, ActorRef.noSender());
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
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

    public Map<String, ActorRef> getChildren() {
        return children;
    }

    public String getPath() {
        return path;
    }

    public void stop() {
        if (state.compareAndSet(LifecycleState.STARTED, LifecycleState.STOPPING)) {
            // 停止所有子Actor
            new ArrayList<>(children.values()).forEach(child -> {
                system.stop(child);
            });

            // 取消所有计时器
            timers.values().forEach(timer -> timer.cancel(false));
            timers.clear();

            // 停止接收超时任务
            if (receiveTimeoutTask != null) {
                receiveTimeoutTask.cancel(false);
            }

            // 关闭调度器
            scheduler.shutdown();

            // 调用Actor的停止回调
            AbstractActor<?> actor = self.get();
            if (actor != null) {
                actor.postStop();
            }

            state.set(LifecycleState.STOPPED);
        }
    }

    // 添加重启逻辑
    public void restart(Throwable reason) {
        AbstractActor<?> actor = self.get();
        if (actor != null) {
            actor.preRestart(reason);
            stop();
            initializeActor();
            actor.postRestart(reason);
        }
    }

    public void stop(ActorRef actorRef){
        if (actorRef instanceof ActorRefProxy) {
            ((ActorRefProxy<?>) actorRef).destroy();
        }
    }

    public ActorContext getParent() {
        return parent;
    }

    public <R> ActorRef<R> actorOf(Props<R> props, String name) {
        validateChildName(name);
        String childPath = self.get().path() + "/" + name;
        ActorRef<R> child = system.actorOf(props, childPath,this);
        children.put(name, child);
        return child;
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduler.schedule(command, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    // 添加定时器功能
    public <T> void scheduleOnce(String key, Duration delay, T message) {
        cancelTimer(key);
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            Envelope envelope=new Envelope(message,ActorRef.noSender(),self.get(), MessageType.NORMAL,1);
            tell(envelope);
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        timers.put(key, timer);
    }

    public void scheduleRepeatedly(String key, Duration initialDelay, Duration interval, Object message) {
        cancelTimer(key);
        ScheduledFuture<?> timer = scheduler.scheduleAtFixedRate(() -> {
            Envelope envelope=new Envelope(message,ActorRef.noSender(),self.get(), MessageType.NORMAL,1);
            tell(envelope);
        }, initialDelay.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        timers.put(key, timer);
    }

    public void cancelTimer(String key) {
        ScheduledFuture<?> timer = timers.remove(key);
        if (timer != null) {
            timer.cancel(false);
        }
    }

}
