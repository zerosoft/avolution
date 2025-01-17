package com.avolution.actor.system.actor;

import com.avolution.actor.core.TypedActor;
import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.metrics.ActorMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 死信 Actor
 * 负责处理系统中无法投递的消息（死信），并记录相关的统计信息和日志。
 */
public class DeadLetterActor extends TypedActor<IDeadLetterActorMessage> {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);
    private static final int MAX_DEAD_LETTERS = 1000; // 最大死信队列容量
    private static final int WARN_THRESHOLD = 100;   // 死信数量警告阈值

    // 最近收到的死信队列
    private final ConcurrentLinkedQueue<IDeadLetterActorMessage.DeadLetter> recentDeadLetters;
    // 按 Actor 统计的死信数量
    private final Map<String, LongAdder> deadLettersByActor;
    // 按消息类型统计的死信数量
    private final Map<MessageType, LongAdder> deadLettersByType;
    // 总死信数量
    private final AtomicInteger totalDeadLetters;
    // 指标收集器
    private final ActorMetricsCollector metricsCollector;

    // Actor 系统实例
    private final ActorSystem actorSystem;

    /**
     * 构造函数
     *
     * @param actorSystem Actor 系统实例
     */
    public DeadLetterActor(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.recentDeadLetters = new ConcurrentLinkedQueue<>();
        this.deadLettersByActor = new ConcurrentHashMap<>();
        this.deadLettersByType = new ConcurrentHashMap<>();
        this.totalDeadLetters = new AtomicInteger(0);
        this.metricsCollector = new ActorMetricsCollector("/system/deadLetters");
    }

    /**
     * 处理死信数量查询消息
     *
     * @param message 死信数量查询消息
     */
    private void handleDeadLetterCount(IDeadLetterActorMessage.DeadLetterCount message) {
        getSender().tell(recentDeadLetters.size(), getSelf());
    }

    /**
     * 处理死信消息
     *
     * @param deadLetter 死信消息
     */
    private void handleDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        try {
            updateMetrics(deadLetter);          // 更新指标
            maintainRecentDeadLetters(deadLetter); // 维护最近死信队列
            logDeadLetter(deadLetter);          // 记录日志
            handleSpecificDeadLetter(deadLetter); // 处理特定类型的死信
        } catch (Exception e) {
            log.error("Error processing dead letter: {}", deadLetter, e);
        }
    }

    /**
     * 维护最近死信队列
     *
     * @param deadLetter 死信消息
     */
    private void maintainRecentDeadLetters(IDeadLetterActorMessage.DeadLetter deadLetter) {
        // 添加新的死信到队列
        recentDeadLetters.offer(deadLetter);

        // 如果队列超过最大容量，移除最旧的死信
        while (recentDeadLetters.size() > MAX_DEAD_LETTERS) {
            IDeadLetterActorMessage.DeadLetter removed = recentDeadLetters.poll();
            if (log.isDebugEnabled()) {
                log.debug("Removed old dead letter from queue: {}", removed);
            }
        }
    }

    /**
     * 记录死信日志
     *
     * @param deadLetter 死信消息
     */
    private void logDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        // 基本日志记录
        log.warn("Dead letter received: {}", deadLetter);

        // 根据消息类型和重试次数添加额外的日志信息
        if (deadLetter.isSystemMessage()) {
            log.error("System message became dead letter: {}", deadLetter);
        }

        if (deadLetter.retryCount() > 0) {
            log.warn("Message retry failed after {} attempts for actor: {}",deadLetter.retryCount(), deadLetter.recipient());
        }

        // 记录失败原因
        String failureReason = deadLetter.getFailureReason();
        if (!"Unknown".equals(failureReason)) {
            log.warn("Dead letter failure reason: {}", failureReason);
        }
    }

    /**
     * 更新死信相关指标
     *
     * @param deadLetter 死信消息
     */
    private void updateMetrics(IDeadLetterActorMessage.DeadLetter deadLetter) {
        int total = totalDeadLetters.incrementAndGet();
        deadLettersByActor.computeIfAbsent(deadLetter.recipient(),
                k -> new LongAdder()).increment();

        deadLettersByType.computeIfAbsent(deadLetter.messageType(),
                k -> new LongAdder()).increment();

        metricsCollector.incrementMessageCount();

        if (total % WARN_THRESHOLD == 0) {
            log.warn("Dead letter count reached: {}", total);
        }
    }

    /**
     * 处理特定类型的死信
     *
     * @param deadLetter 死信消息
     */
    private void handleSpecificDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        switch (deadLetter.messageType()) {
            case SYSTEM -> handleSystemDeadLetter(deadLetter);
            case SIGNAL -> handleSignalDeadLetter(deadLetter);
            default -> handleDefaultDeadLetter(deadLetter);
        }
    }

    /**
     * 处理系统消息类型的死信
     *
     * @param deadLetter 死信消息
     */
    private void handleSystemDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        log.error("System message became dead letter: {}", deadLetter);
        metricsCollector.incrementSignalCount();
    }

    /**
     * 处理信号消息类型的死信
     *
     * @param deadLetter 死信消息
     */
    private void handleSignalDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        log.warn("Signal message became dead letter: {}", deadLetter);
        metricsCollector.incrementSignalCount();
    }

    /**
     * 处理默认类型的死信
     *
     * @param deadLetter 死信消息
     */
    private void handleDefaultDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        if (deadLetter.retryCount() > 0) {
            log.warn("Message retry failed after {} attempts for actor: {}", deadLetter.retryCount(), deadLetter.recipient());
        }
    }

    /**
     * 清理死信队列和统计信息
     */
    private void cleanup() {
        recentDeadLetters.clear();
        deadLettersByActor.clear();
        deadLettersByType.clear();
    }

    /**
     * 处理接收到的消息
     *
     * @param message 接收到的消息
     * @throws Exception 处理消息时可能抛出的异常
     */
    @Override
    protected void onReceive(IDeadLetterActorMessage message) throws Exception {
        switch (message) {
            case IDeadLetterActorMessage.DeadLetter deadLetter -> handleDeadLetter(deadLetter);
            case IDeadLetterActorMessage.DeadLetterCount deadLetterCount -> handleDeadLetterCount(deadLetterCount);
            default -> throw new IllegalArgumentException("Unknown message type: " + message);
        }
    }
}