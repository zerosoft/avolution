package com.avolution.actor.core.context;

import com.avolution.actor.core.*;
import com.avolution.actor.core.lifecycle.ActorContextInternalLifecycleHook;
import com.avolution.actor.core.lifecycle.ActorLifecycle;
import com.avolution.actor.core.lifecycle.InternalLifecycleHook;
import com.avolution.actor.lifecycle.ActorContextLifecycle;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.message.*;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;


/**
 * Actor上下文类
 * 负责管理Actor的生命周期、消息处理和子Actor管理
 */
public class ActorContext implements ActorContextLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ActorContext.class);
    // Actor在系统中的唯一路径标识
    private final String path;
    // Actor所属的系统实例
    private final ActorSystem system;
    // 当前Actor实例
    private final UnTypedActor<?> unTypedActor;
    // 父Actor的上下文，用于构建Actor层级关系
    private final ActorContext parent;
    // Actor的消息邮箱，用于消息队列管理
    private final Mailbox mailbox;
    // 子Actor映射表，保存所有子Actor的引用
    private final Map<String, ActorRef<?>> children = new ConcurrentHashMap<>();
    // Actor的生命周期管理器
    private ActorLifecycle lifecycle;
    // 信号处理器，处理系统信号
    private SignalHandler signalHandler;
    // Actor的调度器，负责消息的调度执行
    private final ActorScheduler scheduler;
    // 内部生命周期钩子，处理生命周期事件
    private InternalLifecycleHook internalLifecycleHook;

    /**
     * 初始化Actor上下文
     * @param path Actor路径
     * @param system Actor系统实例
     * @param self Actor实例
     * @param parent 父Actor上下文
     * @param props Actor配置属性
     */
    public ActorContext(String path, ActorSystem system, UnTypedActor self,
                       ActorContext parent, Props props) {
        this.path = path;
        this.system = system;
        this.unTypedActor = self;
        this.parent = parent;
        this.mailbox = new Mailbox(system, props.throughput());
        this.scheduler = new DefaultActorScheduler();
        this.signalHandler=new SignalHandler(this);

        this.lifecycle =new ActorLifecycle(this,self);
        // 将 lifecycle 传给 InternalLifecycleHook
        this.internalLifecycleHook=new ActorContextInternalLifecycleHook(this,lifecycle);
    }

    /**
     * 发送消息到Actor
     * 如果Actor未终止，将消息加入邮箱；否则作为死信处理
     */
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

    /**
     * 处理死信消息
     * 将消息转换为死信并发送到系统的死信Actor
     */
    private void handleDeadLetter(Envelope envelope) {
        IDeadLetterActorMessage.DeadLetter deadLetter = IDeadLetterActorMessage.messageToDeadLetter(envelope);
        // 记录死信
        logger.warn("Dead letter received: {}", deadLetter);

        // 发送到系统的死信Actor
        system.getDeadLetters().tell(deadLetter, unTypedActor.getSelfRef());
    }

    /**
     * 处理邮箱中的消息
     * 按顺序处理消息，并在必要时重新调度
     */
    public void processMailbox() {
        if (lifecycle.isTerminated()) {
            return;
        }

        try {
            // 处理邮箱中的消息
            while (!mailbox.isSuspended()
                    || mailbox.hasHighPrioritySignals()) {
                // 从邮箱中获取消息
                Envelope message = mailbox.poll();
                if (message == null) {
                    break;
                }

                try {
                    // 设置消息发送者
                    unTypedActor.setSender(message.getSender());
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
     * 处理单个消息
     * 根据消息类型分发到不同的处理器
     */
    private void processMessage(Envelope envelope) {
        try {
            switch (envelope.getMessageType()) {
                case SIGNAL -> handleSignal(envelope);
                case SYSTEM -> handleSystemMessage(envelope);
                case DEAD_LETTER -> handleDeadLetter(envelope);
                default -> unTypedActor.onReceive(envelope.getMessage());
            }
        } catch (Exception e) {
            handleProcessingError(e, envelope);
        }
    }

    private void handleSystemMessage(Envelope envelope) {

    }

    private void handleProcessingError(Exception exception, Envelope envelope) {

    }

    /**
     * 处理系统信号
     */
    private void handleSignal(Envelope envelope) {
        signalHandler.handle(envelope);
    }

    /**
     * 获取所有子Actor的映射
     * @return 子Actor映射表的副本
     */
    public Map<String, ActorRef> getChildren() {
        Map<String, ActorRef> result=new HashMap<>();
        Set<String> keySets = children.keySet();
        for (String keySet : keySets) {
            result.put(keySet,children.get(keySet));
        }
        return result;
    }


    // 生命周期管理方法实现
    @Override
    public boolean start() {
        try {
            internalLifecycleHook.executePreStart();
            return true;
        } catch (Exception e) {
            logger.error("Failed to start actor: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean stop() {
        try {
            CompletableFuture<Void> stopFuture = new CompletableFuture<>();
            lifecycle.stop(stopFuture);
            stopFuture.get(10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.error("Failed to stop actor: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean stopNow() {
        try {
            internalLifecycleHook.executePostStop();
            return true;
        } catch (Exception e) {
            logger.error("Failed to stop actor immediately: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean restart(Throwable cause) {
        try {
            internalLifecycleHook.executePreRestart(cause);
            internalLifecycleHook.executePostRestart(cause);
            return true;
        } catch (Exception e) {
            logger.error("Failed to restart actor: {}", path, e);
            return false;
        }
    }

    /**
     * 恢复Actor
     */
    @Override
    public boolean resume() {
        try {
            internalLifecycleHook.executeResume();
            return true;
        } catch (Exception e) {
            logger.error("Failed to suspend actor: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean suspend() {
        try {
            internalLifecycleHook.executeSuspend();
            return true;
        } catch (Exception e) {
            logger.error("Failed to suspend actor: {}", path, e);
            return false;
        }
    }

    /**
     * 创建子Actor
     * @param props Actor配置
     * @param name Actor名称
     * @return 新创建的ActorRef
     */
    public <R> ActorRef<R> actorOf(Props<R> props, String name) {
        validateChildName(name);
        ActorRef<R> child = system.actorOf(props, name,this);
        children.put(name, child);
        return child;
    }


    /**
     * 监视指定Actor
     * 当目标Actor终止时接收Terminated信号
     */
    public void watch(ActorRef<?> target) {
        watch(target, () -> {
            // 默认处理：接收 Terminated 信号
            unTypedActor.tell(Signal.TERMINATED, target);
        });
    }

    /**
     * 监视指定Actor，并提供自定义回调
     */
    public void watch(ActorRef<?> target, Runnable callback) {
        if (target == null || callback == null) {
            return;
        }

        // 转换为 DeathWatch 回调
        system.getDeathWatch().watch(unTypedActor.getSelfRef(), target, (terminated, normal) -> {
            try {
                callback.run();
            } catch (Exception e) {
                logger.error("Error executing watch callback for {}", target.path(), e);
            }
        });
    }

    /**
     * 取消监视指定Actor
     */
    public void unwatch(ActorRef<?> target) {
        if (target != null) {
            system.getDeathWatch().unwatch(unTypedActor.getSelfRef(), target);
        }
    }

    /**
     * 验证子Actor名称的有效性
     */
    private void validateChildName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Child name cannot be null or empty");
        }
        if (children.containsKey(name)) {
            throw new IllegalArgumentException("Child with name " + name + " already exists");
        }
    }

    // Getter方法
    public UnTypedActor getUnTypedActor() {
        return unTypedActor;
    }

    public IScheduler getScheduler() {
        return scheduler;
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


    public void removeChild(ActorRef<?> child) {
        if (child != null) {
            children.remove(child.name());
        }
    }

    public CompletableFuture<Void> stop(CompletableFuture<Void> stop){
        return lifecycle.stop(stop);
    }

    /**
     * 停止Actor (自己或者子类 )
     * @param actor
     * @return
     */
    public CompletableFuture<Void> stop(ActorRef actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        // 直接调用生命周期管理
        if (actor.path().equals(getUnTypedActor().path())) {
            return lifecycle.stop(stopFuture);
        }

        // 子Actor停止逻辑
        if (children.containsKey(actor.name())) {
            Envelope signal = Envelope.builder()
                    .message(Signal.POISON_PILL)
                    .priority(Priority.HIGH)
                    .scope(SignalScope.SINGLE)
                    .type(MessageType.SIGNAL)
                    .build();
            signal.addMetadata("stopFuture", stopFuture);

            actor.tell(signal, getUnTypedActor().getSelfRef());

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
            Envelope kill = Envelope.builder()
                    .message(Signal.KILL)
                    .priority(Priority.HIGH)
                    .scope(SignalScope.SINGLE)
                    .build();
            actor.tell(kill, getUnTypedActor().getSelfRef());
        }
    }
}
