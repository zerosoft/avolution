package com.avolution.actor.core.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;
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
    // Actor配置属性
    private Props props;
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
        this.props=props;
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
            logger.debug("Starting actor: {}", path);
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
     * 修改现有的stop方法，使用gracefulStop
     */
    @Override
    public boolean stop(boolean now) {
        if (now) {
            // 立即停止
            mailbox.close();
            return internalLifecycleHook.executeStop();
        } else {
            // 优雅停止
            gracefulStop().exceptionally(throwable -> {
                logger.error("Error during graceful stop of actor: {}", path, throwable);
                return null;
            });
            return true;
        }
    }

    /**
     * 优雅关闭Actor及其子Actor
     * @return 关闭完成的Future
     */
    public CompletableFuture<Void> gracefulStop() {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        if (children.isEmpty()) {
            // 没有子Actor，直接关闭自己
            stopSelf(stopFuture);
            return stopFuture;
        }

        // 创建计数器跟踪子Actor关闭状态
        AtomicInteger remainingChildren = new AtomicInteger(children.size());
        List<CompletableFuture<Void>> childStopFutures = new ArrayList<>();

        // 停止所有子Actor
        for (ActorRef<?> child : children.values()) {
            CompletableFuture<Void> childStopFuture = stopChild(child);
            childStopFutures.add(childStopFuture);

            childStopFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Error stopping child actor: {}", child.path(), throwable);
                }

                if (remainingChildren.decrementAndGet() == 0) {
                    // 所有子Actor已关闭，关闭自己
                    stopSelf(stopFuture);
                }
            });
        }

        // 设置超时处理
        scheduleStopTimeout(stopFuture, childStopFutures);

        return stopFuture;
    }
    /**
     * 停止子Actor
     */
    private CompletableFuture<Void> stopChild(ActorRef child) {
        CompletableFuture<Void> childStopFuture = new CompletableFuture<>();

        Envelope stopSignal = Envelope.builder()
                .message(Signal.STOP)
                .type(MessageType.SIGNAL)
                .priority(Priority.HIGH)
                .scope(SignalScope.SINGLE)
                .sender(unTypedActor.getSelfRef())
                .recipient(child)
                .build();

        // 添加停止Future到元数据中
        stopSignal.addMetadata("stopFuture", childStopFuture);

        // 发送停止信号
        child.tell(stopSignal, ActorRef.noSender());

        return childStopFuture;
    }

    /**
     * 停止当前Actor
     */
    private void stopSelf(CompletableFuture<Void> stopFuture) {
        try {
            // 关闭邮箱
            mailbox.close();

            // 执行停止生命周期钩子
            boolean stopped = internalLifecycleHook.executeStop();

            if (stopped) {
                // 从父Actor中移除自己
                if (parent != null) {
                    parent.removeChild(unTypedActor.getSelfRef());
                }
                stopFuture.complete(null);
            } else {
                stopFuture.completeExceptionally(
                        new IllegalStateException("Failed to stop actor: " + path)
                );
            }
        } catch (Exception e) {
            stopFuture.completeExceptionally(e);
        }
    }

    /**
     * 设置停止超时处理
     */
    private void scheduleStopTimeout(
            CompletableFuture<Void> stopFuture,
            List<CompletableFuture<Void>> childStopFutures) {

        // 设置30秒超时
        scheduler.schedule(() -> {
            if (!stopFuture.isDone()) {
                // 取消所有未完成的子Actor停止Future
                childStopFutures.forEach(future ->
                        future.completeExceptionally(
                                new TimeoutException("Actor stop timeout: " + path)
                        )
                );

                // 强制停止
                mailbox.close();
                stop(true);
                stopFuture.completeExceptionally(
                        new TimeoutException("Actor stop timeout: " + path)
                );
            }
        }, 30, TimeUnit.SECONDS);
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
        // 获取监督策略
        SupervisorStrategy strategy = getUnTypedActor().getSupervisorStrategy();

        // 根据策略决定处理方式
        Directive directive = strategy.handle(failure.getCause());

        switch (directive) {
            // 继续处理下一条消息
            case RESUME ->
                logger.debug("Resuming actor after failure: {}", failure);
//                    child.tell(Signal.RESUME, getSelf());
            // 重启Actor
            case RESTART ->
                logger.debug("Restarting actor after failure: {}", failure);
//                    child.tell(Signal.RESTART, getSelf());
            //  停止Actor
            case STOP -> {
                logger.debug("Stopping actor after failure: {}", failure);
                ActorRef actorRef = failure.getFailedActor();
                logger.debug("Stopping actor after failure: {}", actorRef.path());
                gracefulStop()
                        .exceptionally(e -> {
                            logger.error("Error stopping actor {} after failure",
                                    actorRef.path(), e);
                            return null;
                        });
            }
            //  向上传递错误
            case ESCALATE -> {
                // 向父Actor升级错误
                if (getParent() != null) {
//                    Envelope escalateSignal = createEscalateSignal(cause, child, failedMessage);
//                    getParent().getUnTypedActor().tell(escalateSignal);
                }
            }
        }
    }

    /**
     * 停止Actor (自己或者子类 )
     *
     * @param actor
     * @return
     */
    public void stop(ActorRef actor) {
        CompletableFuture<Void> stopFuture = new CompletableFuture<>();

        // 直接调用生命周期管理
        if (actor.path().equals(getUnTypedActor().path())) {
            lifecycle.stop();
            return;
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

            return ;
        }

        stopFuture.complete(null);
        return;
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
}


