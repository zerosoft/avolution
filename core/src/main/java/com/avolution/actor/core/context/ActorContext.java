package com.avolution.actor.core.context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorScheduler;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.DefaultActorScheduler;
import com.avolution.actor.core.IScheduler;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.lifecycle.ActorContextInternalLifecycleHook;
import com.avolution.actor.core.lifecycle.ActorLifecycle;
import com.avolution.actor.core.lifecycle.InternalLifecycleHook;
import com.avolution.actor.lifecycle.ActorContextLifecycle;
import com.avolution.actor.mailbox.Mailbox;
import com.avolution.actor.mailbox.MailboxConfig;
import com.avolution.actor.mailbox.MetricsSnapshot;
import com.avolution.actor.message.ActorFailure;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Priority;
import com.avolution.actor.message.Signal;
import com.avolution.actor.message.SignalScope;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;


/**
 * Actor上下文类
 * 负责管理Actor的生命周期、消息处理和子Actor管理
 */
public class ActorContext implements ActorContextLifecycle,IActorContext {
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
     *
     * @param path         Actor路径
     * @param system       Actor系统实例
     * @param unTypedActor Actor实例
     * @param parent       父Actor上下文
     * @param props        Actor配置属性
     */
    public ActorContext(String path, ActorSystem system, UnTypedActor unTypedActor,
                        ActorContext parent, Props props) {
        this.path = path;
        this.system = system;
        this.unTypedActor = unTypedActor;
        this.parent = parent;

        // 创建邮箱配置
        MailboxConfig config = MailboxConfig.builder()
                .capacity(2000)                // 设置较大的容量
                .throughputLimit(200)          // 适当的吞吐量限制
                .retryAttempts(5)             // 充分的重试次数
                .messageTimeout(60000)         // 合理的超时时间
                .build();

        this.mailbox = new Mailbox(config);
        this.scheduler = new DefaultActorScheduler();
        this.signalHandler = new SignalHandler(this);

        this.lifecycle = new ActorLifecycle(this, unTypedActor);
        this.internalLifecycleHook = new ActorContextInternalLifecycleHook(this, lifecycle);
    }

    /**
     * 发送消息到Actor
     */
    public void tell(Envelope envelope) {
        if (!lifecycle.isTerminated()) {
            try {
                boolean enqueued = mailbox.enqueue(envelope);
                if (!enqueued) {
                    logger.warn("Failed to enqueue message, mailbox is full: {}", envelope);
                    handleDeadLetter(envelope);
                    return;
                }

                // 如果邮箱中有消息，则调度处理
                if (mailbox.hasMessages()) {
                    try {
                        system.dispatcher().dispatch(path, this::processMailbox);
                    } catch (Exception e) {
                        logger.error("Failed to dispatch message processing for actor: {}", path, e);
                        handleDeadLetter(envelope);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to enqueue message: {}", envelope, e);
                handleDeadLetter(envelope);
            }
        } else {
            handleDeadLetter(envelope);
        }
    }

    /**
     * 暂存消息
     */
    public void stash(Envelope envelope) {
        mailbox.stash(envelope);
    }

    /**
     * 取回暂存的消息
     */
    public void unstash() {
        mailbox.unstashAll();
    }

    /**
     * 清空邮箱
     */
    public void clearMailbox() {
        mailbox.clear();
    }

    /**
     * 获取邮箱状态
     */
    public boolean hasMessages() {
        return mailbox.hasMessages();
    }

    /**
     * 获取邮箱指标
     */
    public MetricsSnapshot getMailboxMetrics() {
        return mailbox.getMetrics().getSnapshot();
    }


    // 生命周期管理方法实现
    @Override
    public boolean start() {
        try {
            // 执行生命周期钩子
            return internalLifecycleHook.executeStart();
        } catch (Exception e) {
            logger.error("Failed to start actor: {}", path, e);
            return false;
        }
    }

    /**
     * 暂停Actor处理
     */
    @Override
    public boolean suspend() {
        try {
            mailbox.suspend();
            return internalLifecycleHook.executeSuspend();
        } catch (Exception e) {
            logger.error("Failed to suspend actor: {}", path, e);
            return false;
        }
    }

    /**
     * 恢复Actor处理
     */
    @Override
    public boolean resume() {
        try {
            mailbox.resume();
            return internalLifecycleHook.executeResume();
        } catch (Exception e) {
            logger.error("Failed to resume actor: {}", path, e);
            return false;
        }
    }

    /**
     * 停止Actor
     */
    @Override
    public boolean stop(boolean now) {
        if (now){
            try {
                clearMailbox();
                return internalLifecycleHook.executeStop();
            } catch (Exception e) {
                logger.error("Failed to stop actor immediately: {}", path, e);
                return false;
            }
        }else {
            CompletableFuture<Void> stopFuture = new CompletableFuture<>();
            try {
                // 清空邮箱
                clearMailbox();

                // 停止Actor
                lifecycle.stop(stopFuture);

                // 等待停止完成
                stopFuture.get(mailbox.getMessageTimeout(), TimeUnit.MILLISECONDS);

                return true;
            } catch (TimeoutException te) {
                logger.error("Actor stop timed out: {}", path, te);
                stopFuture.completeExceptionally(te);
                return false;
            } catch (Exception e) {
                logger.error("Failed to stop actor: {}", path, e);
                stopFuture.completeExceptionally(e);
                return false;
            } finally {
                if (!stopFuture.isDone()) {
                    stopFuture.complete(null); // 确保 future 完成
                }
            }
        }

    }


    @Override
    public boolean restart(Throwable cause) {
        try {
            internalLifecycleHook.executeRestart(cause);
            return true;
        } catch (Exception e) {
            logger.error("Failed to restart actor: {}", path, e);
            return false;
        }
    }


    /**
     * 创建子Actor
     */
    public <T> ActorRef<T> actorOf(Props<T> props, String name) {
        return system.actorOf(props, name, this);
    }

    /**
     * 处理邮箱中的消息
     * 按优先级和批次处理消息，并在必要时重新调度
     */
    public void processMailbox() {
        if (lifecycle.isTerminated()) {
            return;
        }

        try {
            int processedCount = 0;
            int batchLimit = mailbox.getThroughputLimit();

            while (processedCount < batchLimit && !lifecycle.isTerminated()) {
                Envelope envelope = mailbox.dequeue();
                if (envelope == null) {
                    break;
                }

                try {
                    processedCount++;
                    long startTime = System.nanoTime();

                    unTypedActor.setSender(envelope.getSender());
                    processMessage(envelope);

                    // 记录处理时间
                    mailbox.getMetrics().recordProcessingTime(System.nanoTime() - startTime);

                } catch (Exception e) {
                    handleProcessingError(e, envelope);
                }
            }

            // 如果邮箱中还有消息，重新调度处理
            if (mailbox.hasMessages()) {
                system.dispatcher().dispatch(path, this::processMailbox);
            }
        } catch (Exception e) {
            logger.error("Error processing mailbox for actor: {}", path, e);
            mailbox.getMetrics().recordError();
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
                default -> unTypedActor.onReceive(envelope);
            }
        } catch (Exception e) {
            handleProcessingError(e, envelope);
        }
    }

    /**
     * 处理系统信号
     */
    private void handleSignal(Envelope envelope) {
        signalHandler.handle(envelope);
    }

    /**
     * 处理系统消息
     */
    private void handleSystemMessage(Envelope envelope) {
        try {
            Object message = envelope.getMessage();
            if (message instanceof ActorFailure) {
                handleActorFailure((ActorFailure) message);
            } else {
//                unTypedActor.onSystemMessage(message);
            }
        } catch (Exception e) {
            logger.error("Error handling system message: {}", envelope, e);
            mailbox.getMetrics().recordError();
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
     * 处理消息处理过程中的错误
     */
    private void handleProcessingError(Exception exception, Envelope envelope) {
        logger.error("Error processing message: {} for actor: {}", envelope, path, exception);
        mailbox.getMetrics().recordError();

        try {
            // 检查是否应该重试
            if (shouldRetryMessage(envelope)) {
                handleRetry(envelope, exception);
            } else {
                handleFinalFailure(envelope, exception);
            }
        } catch (Exception e) {
            logger.error("Error handling message failure", e);
            system.handleSystemFailure(e, unTypedActor.getSelfRef());
        }
    }

    /**
     * 检查消息是否应该重试
     */
    private boolean shouldRetryMessage(Envelope envelope) {
        return envelope.getRetryCount() < mailbox.getRetryAttempts();
    }

    /**
     * 处理消息重试
     */
    private void handleRetry(Envelope envelope, Exception e) {
        envelope.incrementRetryCount();
        envelope.setLastError(e);

        // 计算重试延迟
        long delay = calculateRetryDelay(envelope.getRetryCount());

        // 调度重试
        scheduler.schedule(() -> {
            if (!lifecycle.isTerminated()) {
                tell(envelope);
            }
        }, delay, TimeUnit.MILLISECONDS);

        logger.debug("Scheduled retry #{} for message: {} after {}ms",
                envelope.getRetryCount(), envelope, delay);
    }

    /**
     * 计算重试延迟时间（使用指数退避策略）
     */
    private long calculateRetryDelay(int retryCount) {
        long baseDelay = mailbox.getRetryDelayMs();
        return Math.min(baseDelay * (1L << (retryCount - 1)), 30000); // 最大30秒
    }

    /**
     * 处理最终失败的消息
     */
    private void handleFinalFailure(Envelope envelope, Exception e) {
        logger.error("Message processing failed after {} retries: {}",
                envelope.getRetryCount(), envelope, e);

        // 通知监督者
        if (parent != null) {
            Envelope failure = Envelope.builder()
                    .message(new ActorFailure(unTypedActor.getSelfRef(), e, envelope))
                    .type(MessageType.SYSTEM)
                    .priority(Priority.HIGH)
                    .sender(unTypedActor.getSelfRef())
                    .recipient(parent.getUnTypedActor().getSelfRef())
                    .build();

            parent.tell(failure);
        }

        // 发送到死信队列
        handleDeadLetter(envelope);
    }


    /**
     * 处理Actor失败消息
     */
    private void handleActorFailure(ActorFailure failure) {
        ActorRef failedActor = failure.getFailedActor();
        if (children.containsValue(failedActor)) {
//            unTypedActor.supervisorStrategy().handleFailure(this, failedActor, failure.getCause(), failure.getFailedMessage());
        }
    }


    /**
     * 获取所有子Actor的映射
     *
     * @return 子Actor映射表
     */
    public Map<String, ActorRef<?>> getChildren() {
        return children;
    }

    /**
     * 获取所有子Actor的只读映射
     *
     * @return 子Actor只读映射表
     */
    public Map<String, ActorRef<?>> getChildrenView() {
        return Collections.unmodifiableMap(children);
    }


    /**
     * 停止Actor (自己或者子类 )
     *
     * @param actor
     * @return
     */
    public CompletableFuture<Void> stop(ActorRef actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        // 直接调用生命周期管理
        if (actor.path().equals(getUnTypedActor().path())) {
            lifecycle.stop(stopFuture);
            return stopFuture;
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
            // 发送停止信号
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
        // 如果目标Actor或回调为空，则不处理
        if (target == null || callback == null) {
            return;
        }
        // 转换为 DeathWatch 回调
        system.getDeathWatch()
                .watch(unTypedActor.getSelfRef(), target, (terminated, normal) -> {
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


    public void removeChild(ActorRef<?> child) {
        if (child != null) {
            children.remove(child.name());
            // 从系统中注销
            system.unregisterActor(child.path());
        }
    }

    public void stop(CompletableFuture<Void> stop) {
        lifecycle.stop(stop);
    }

    /**
     * 获取父Actor或系统Actor作为监督者
     */
    private ActorRef getParentOrSystem() {
        if (parent != null) {
            return parent.getUnTypedActor().getSelfRef();
        }
        return system.getDeadLetters();
    }


    /**
     * 将错误升级到监督者
     */
    public void escalate(Throwable error, Envelope envelope) {
        if (error == null) {
            logger.warn("Cannot escalate null error");
            return;
        }

        try {
            // 获取监督者
            ActorRef supervisor = getParentOrSystem();
            // 创建Actor失败消息
            ActorFailure failure = new ActorFailure(unTypedActor.getSelfRef(),error,envelope);

            // 发送到监督者
            supervisor.tell(failure, unTypedActor.getSelfRef());
            logger.debug("Escalated error to supervisor: {} - Error: {}",supervisor.path(), error.getMessage());
        } catch (Exception e) {
            logger.error("Failed to escalate error to supervisor", e);
            handleEscalationFailure(error, envelope, e);
        }
    }

    /**
     * 处理升级失败的情况
     */
    private void handleEscalationFailure(Throwable originalError,
                                         Envelope originalEnvelope,
                                         Exception escalationError) {
        try {
            // 记录错误
            logger.error("Multiple failures in actor: {}", path, originalError);
            logger.error("Escalation failure", escalationError);

            // 创建致命错误消息
            IDeadLetterActorMessage.FatalActorError fatalError = new IDeadLetterActorMessage.FatalActorError(
                    unTypedActor.getSelfRef(),
                    originalError,
                    escalationError,
                    originalEnvelope
            );

            // 发送到死信队列
            system.getDeadLetters().tell(fatalError, unTypedActor.getSelfRef());

            // 尝试停止当前Actor
            stop(true);

        } catch (Exception e) {
            logger.error("Critical system failure in actor: {}", path, e);
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
}


