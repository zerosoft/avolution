package com.avolution.actor.system.actor;

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

public class DeadLetterActor extends UnTypedActor<IDeadLetterActorMessage> {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);
    private static final int MAX_DEAD_LETTERS = 1000;
    private static final int WARN_THRESHOLD = 100;

    private final ConcurrentLinkedQueue<IDeadLetterActorMessage.DeadLetter> recentDeadLetters;
    private final Map<String, LongAdder> deadLettersByActor;
    private final Map<MessageType, LongAdder> deadLettersByType;
    private final AtomicInteger totalDeadLetters;
    private final ActorMetricsCollector metricsCollector;

    private final ActorSystem actorSystem;

    public DeadLetterActor(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.recentDeadLetters = new ConcurrentLinkedQueue<>();
        this.deadLettersByActor = new ConcurrentHashMap<>();
        this.deadLettersByType = new ConcurrentHashMap<>();
        this.totalDeadLetters = new AtomicInteger(0);
        this.metricsCollector = new ActorMetricsCollector("/system/deadLetters");
    }

    @OnReceive(IDeadLetterActorMessage.DeadLetter.class)
    private void handleDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        try {
            updateMetrics(deadLetter);
            maintainRecentDeadLetters(deadLetter);
            logDeadLetter(deadLetter);
            handleSpecificDeadLetter(deadLetter);
        } catch (Exception e) {
            log.error("Error processing dead letter: {}", deadLetter, e);
        }
    }

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

    private void logDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        // 基本日志记录
        log.warn("Dead letter received: {}", deadLetter);

        // 根据消息类型和重试次数添加额外的日志信息
        if (deadLetter.isSystemMessage()) {
            log.error("System message became dead letter: {}", deadLetter);
        }

        if (deadLetter.retryCount() > 0) {
            log.warn("Message retry failed after {} attempts for actor: {}",
                    deadLetter.retryCount(), deadLetter.recipient());
        }

        // 记录失败原因
        String failureReason = deadLetter.getFailureReason();
        if (!"Unknown".equals(failureReason)) {
            log.warn("Dead letter failure reason: {}", failureReason);
        }
    }

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


    private void handleSpecificDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        switch (deadLetter.messageType()) {
            case SYSTEM -> handleSystemDeadLetter(deadLetter);
            case SIGNAL -> handleSignalDeadLetter(deadLetter);
            default -> handleDefaultDeadLetter(deadLetter);
        }
    }

    private void handleSystemDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        log.error("System message became dead letter: {}", deadLetter);
        metricsCollector.incrementSignalCount();
    }

    private void handleSignalDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        log.warn("Signal message became dead letter: {}", deadLetter);
        metricsCollector.incrementSignalCount();
    }

    private void handleDefaultDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        if (deadLetter.retryCount() > 0) {
            log.warn("Message retry failed after {} attempts for actor: {}",
                    deadLetter.retryCount(), deadLetter.recipient());
        }
    }

    private void cleanup() {
        recentDeadLetters.clear();
        deadLettersByActor.clear();
        deadLettersByType.clear();
    }
}