package com.avolution.actor.context;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import com.avolution.actor.core.*;
import com.avolution.actor.exception.ActorStopException;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.message.SupervisionMessage;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    private boolean isRoot(){
        return parent == this;
    }

    // Actor关系管理
    private final Map<String, AbstractActor> children;

    private final Set<ActorRef<?>> watchedActors;

    private SupervisorStrategy supervisorStrategy;
    
    // 消息处理
    protected final Mailbox mailbox;

    private final AtomicReference<LifecycleState> state;

    private final ScheduledExecutorService scheduler;

    // 添加新的计时器管理
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public ActorContext(String path, ActorSystem system, AbstractActor<?> self, ActorContext parent,
                        Props<?> props) {
        this.path = path;
        this.system = system;
        this.self = self;
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

    private void initializeActor() {
        self.initialize(this);
        if (state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTED)) {
            self.preStart();
        }
    }

    public void tell(Envelope envelope) {
        if (state.get() == LifecycleState.STARTED) {
            // 将消息放入邮箱
            mailbox.enqueue(envelope);
            // 如果邮箱中只有当前消息，则立即处理
            system.dispatcher().dispatch(path, this::processMailbox);
        }
    }

    private void processMailbox() {
        if (state.get() != LifecycleState.STARTED) {
            return;
        }
        mailbox.process(self);

        if (mailbox.hasMessages()) {
            system.dispatcher().dispatch(path, this::processMailbox);
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
        system.deathWatch().watch(self, other);
    }

    public void unwatch(ActorRef<?> other) {
        watchedActors.remove(other);
        system.deathWatch().unwatch(self, other);
    }

    public void notifyWatchers(ActorRef<?> other) {
        watchedActors.remove(other);
        system.deathWatch().terminated(other);
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
        Map<String, ActorRef> result=new HashMap<>();
        Set<String> keySets = children.keySet();
        for (String keySet : keySets) {
            result.put(keySet,children.get(keySet).getSelf());
        }
        return result;
    }

    public String getPath() {
        return path;
    }

    public void stop() {
        if (state.compareAndSet(LifecycleState.STARTED, LifecycleState.STOPPING)) {

            // 停止所有子Actor
            new ArrayList<>(children.values()).forEach(child -> {
                child.tell(PoisonPill.INSTANCE, self.getSelf());
                children.remove(child.getSelf().name());
            });

            // 取消所有计时器
            timers.values().forEach(timer -> timer.cancel(false));
            timers.clear();

            // 关闭调度器
            scheduler.shutdown();
            // 从系统中注销Actor
            system.unregisterActor(path);
            state.set(LifecycleState.STOPPED);
        }
    }

    // 添加重启逻辑
    public void restart(Throwable reason) {
        self.onPreRestart(reason);
        stop();
        initializeActor();
        self.onPostRestart(reason);
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

            // 处理未完成的消息
            actorRef.tell(PoisonPill.INSTANCE, ActorRef.noSender());

            logger.debug("Successfully stopped actor: {}", actorPath);
        } catch (Exception e) {
            logger.error("Error while stopping actor: {}", actorPath, e);
            // 确保即使发生错误也能移除Actor
            throw new ActorStopException("Failed to stop actor: " + actorPath, e);
        }
    }

    public ActorContext getParent() {
        return parent;
    }

    public <R> ActorRef<R> actorOf(Props<R> props, String name) {
        validateChildName(name);
        String childPath = path + "/" + name;
        ActorRef<R> child = system.actorOf(props, childPath,this);
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
            Envelope envelope=new Envelope(message,ActorRef.noSender(),self.getSelf(), MessageType.NORMAL,1);
            tell(envelope);
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        timers.put(key, timer);
    }

    public void scheduleRepeatedly(String key, Duration initialDelay, Duration interval, Object message) {
        cancelTimer(key);
        ScheduledFuture<?> timer = scheduler.scheduleAtFixedRate(() -> {
            Envelope envelope=new Envelope(message,ActorRef.noSender(),self.getSelf(), MessageType.NORMAL,1);
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

    public void resume() {
        if (self.getSelf() instanceof AbstractActor actor) {
//            actor.resume();
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
}
