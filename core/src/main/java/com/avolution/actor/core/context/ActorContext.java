package com.avolution.actor.core.context;

import com.avolution.actor.core.*;
import com.avolution.actor.core.lifecycle.ActorLifecycle;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.*;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import com.avolution.actor.util.ActorPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Actor上下文
 */
public class ActorContext  {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);

    private final String path;                    // Actor路径
    private final ActorSystem system;
    // Actor系统引用
    private final AbstractActor self;          // Actor实例

    private final ActorContext parent;            // 父Actor上下文
    private final Mailbox mailbox;                // 消息邮箱

    private final Map<String, ActorRef> children = new ConcurrentHashMap<>();  // 子Actor映射
    // 生命周期
    private ActorLifecycle lifecycle;
    // 信号处理器
    private SignalHandler signalHandler;

    private final ActorScheduler scheduler;        // 调度器

    /**
     * 初始化Actor上下文
     * @param path Actor路径
     * @param system Actor系统
     * @param self Actor实例
     * @param parent 父Actor上下文
     * @param props Actor属性配置
     */
    public ActorContext(String path, ActorSystem system, AbstractActor<?> self, ActorContext parent, Props props) {
        this.path = path;
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.mailbox = new Mailbox(system, props.throughput());
        this.scheduler = new DefaultActorScheduler();
        this.lifecycle =new ActorLifecycle(this);
        this.signalHandler=new SignalHandler(this,lifecycle);
    }


    public void tell(Envelope envelope) {
        if (!lifecycle.isTerminated()) {
            mailbox.enqueue(envelope);
            if (mailbox.hasMessages()) {
                system.dispatcher().dispatch(path, this::processMailbox);
            }
        } else {
            handleDeadLetter(envelope);
        }
    }

    private void handleDeadLetter(Envelope envelope) {
        IDeadLetterActorMessage.DeadLetter deadLetter = IDeadLetterActorMessage.messageToDeadLetter(envelope);
        // 记录死信
        logger.warn("Dead letter received: {}", deadLetter);

        // 发送到系统的死信Actor
        system.getDeadLetters().tell(deadLetter, self.getSelfRef());
    }

    /**
     * 处理邮箱中的消息
     * 确保消息按顺序处理，并在必要时重新调度
     */
    public void processMailbox() {
        if (lifecycle.isTerminated()) {
            return;
        }

        try {
            // 处理邮箱中的消息
            while (!mailbox.isSuspended() || mailbox.hasHighPrioritySignals()) {
                // 从邮箱中获取消息
                Envelope message = mailbox.poll();
                if (message == null) {
                    break;
                }

                try {
                    // 设置消息发送者
                    self.setSender(message.getSender());
                    processMessage(message);
                } catch (Exception e) {
                    handleProcessingError(e, message);
                }
            }

            if (mailbox.hasMessages()) {
                system.dispatcher().dispatch(path, this::processMailbox);
            }
        } catch (Exception e) {
            logger.error("Error processing mailbox for actor: {}", path, e);
        }
    }

    /**
     * 处理消息
     * @param envelope
     */
    private void processMessage(Envelope envelope) {
        if (envelope instanceof SignalEnvelope signalEnvelope) {
            signalHandler.handle(signalEnvelope);
        } else {
            self.onReceive(envelope.getMessage());
        }
    }

    private void handleProcessingError(Exception e, Envelope envelope) {
        logger.error("Error processing message: {}", envelope, e);
    }

    /**
     * 获取子Actor的映射
     * @return
     */
    public Map<String, ActorRef> getChildren() {
        Map<String, ActorRef> result=new HashMap<>();
        Set<String> keySets = children.keySet();
        for (String keySet : keySets) {
            result.put(keySet,children.get(keySet));
        }
        return result;
    }


    public <R> ActorRef<R> actorOf(Props<R> props, String name) {
        validateChildName(name);
        ActorRef<R> child = system.actorOf(props, name,this);
        children.put(name, child);
        return child;
    }

    /**
     * 创建启动Actor
     */
    public void start() {
        lifecycle.start();
    }

    /**
     * 恢复Actor
     */
    public void resume() {
        lifecycle.resume();
    }


    public void watch(ActorRef<?> target) {
        watch(target, () -> {
            // 默认处理：接收 Terminated 信号
            self.tell(Signal.TERMINATED, target);
        });
    }

    public void watch(ActorRef<?> target, Runnable callback) {
        if (target == null || callback == null) {
            return;
        }

        // 转换为 DeathWatch 回调
        system.getDeathWatch().watch(self.getSelfRef(), target, (terminated, normal) -> {
            try {
                callback.run();
            } catch (Exception e) {
                logger.error("Error executing watch callback for {}", target.path(), e);
            }
        });
    }

    public void unwatch(ActorRef<?> target) {
        if (target != null) {
            system.getDeathWatch().unwatch(self.getSelfRef(), target);
        }
    }


    private void validateChildName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Child name cannot be null or empty");
        }
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Child with name " + name + " already exists");
        }
    }

    public AbstractActor getSelf() {
        return self;
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

    public ActorSystem getActorSystem() {
        return system;
    }

    public String getPath() {
        return path;
    }

    public ActorLifecycle getLifecycle() {
        return lifecycle;
    }

    public ActorContext getParent() {
        return parent;
    }

    /**
     * 清理资源
     */
    public void cleanup() {

    }

    public void removeChild(ActorRef<?> child) {
        if (child != null) {
            children.remove(child.name());
        }
    }

    /**
     * 停止Actor (自己或者子类 )
     * @param actor
     * @return
     */
    public CompletableFuture<Void> stop(ActorRef actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        // 直接调用生命周期管理
        if (actor.path().equals(getSelf().path())) {
            return lifecycle.stop(stopFuture);
        }

        // 子Actor停止逻辑
        if (children.containsKey(actor.name())) {
            SignalEnvelope signal = SignalEnvelope.builder()
                    .signal(Signal.POISON_PILL)
                    .priority(Envelope.Priority.HIGH)
                    .scope(SignalScope.SINGLE)
                    .build();
            signal.addMetadata("stopFuture", stopFuture);
            actor.tell(signal, getSelf().getSelfRef());

            return stopFuture.orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(e -> {
                        handleStopTimeout(actor, e);
                        return null;
                    });
        }

        stopFuture.complete(null);
        return stopFuture;
    }

    private void handleStopTimeout(ActorRef actor, Throwable e) {
        if (e instanceof TimeoutException) {
            logger.warn("Actor stop timeout: {}", actor.path());
            SignalEnvelope kill = SignalEnvelope.builder()
                    .signal(Signal.KILL)
                    .priority(Envelope.Priority.HIGH)
                    .scope(SignalScope.SINGLE)
                    .build();
            actor.tell(kill, getSelf().getSelfRef());
        }
    }
}
