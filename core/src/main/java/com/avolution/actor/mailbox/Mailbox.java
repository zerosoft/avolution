package com.avolution.actor.mailbox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.exception.MailboxException;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.SignalEnvelope;
import com.avolution.actor.message.SignalPriority;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor消息邮箱实现
 */
public class Mailbox {
    private static final Logger logger = LoggerFactory.getLogger(Mailbox.class);

    private final PriorityBlockingQueue<Envelope<?>> messages;
    private final AtomicBoolean suspended = new AtomicBoolean(false);
    private final AtomicReference<Thread> processingThread = new AtomicReference<>();
    private volatile Envelope<?> currentMessage;
    private final ActorSystem actorSystem;

    public Mailbox(ActorSystem actorSystem,int capacity) {
        this.actorSystem=actorSystem;
        this.messages = new PriorityBlockingQueue<>(capacity,
                (e1, e2) -> {
                    if (e1 instanceof SignalEnvelope && e2 instanceof SignalEnvelope) {
                        return ((SignalEnvelope) e1).priority().compareTo(((SignalEnvelope) e2).priority());
                    }
                    return e1.getPriority().compareTo(e2.getPriority());
                });
    }

    public void enqueue(Envelope<?> envelope) {
        if (suspended.get()) {
            if (envelope instanceof SignalEnvelope &&
                    ((SignalEnvelope) envelope).priority() == SignalPriority.HIGH) {
                // 高优先级信号即使在暂停状态也允许入队
                offerMessage(envelope);
            }
            return;
        }
        offerMessage(envelope);
    }

    private void offerMessage(Envelope<?> envelope) {
        if (!messages.offer(envelope, 100, TimeUnit.MILLISECONDS)) {
            logger.warn("Failed to enqueue message due to timeout: {}", envelope);
        }
    }

    public void process(AbstractActor actor) {
        if (suspended.get() && !hasHighPrioritySignals()) {
            return;
        }

        processingThread.set(Thread.currentThread());
        try {
            while (!suspended.get() || hasHighPrioritySignals()) {
                currentMessage = messages.poll();
                if (currentMessage == null) {
                    break;
                }

                try {
                    processMessage(actor, currentMessage);
                } catch (Exception e) {
                    handleProcessingError(actor, e);
                } finally {
                    currentMessage = null;
                }
            }
        } finally {
            processingThread.set(null);
        }
    }

    private void processMessage(AbstractActor actor, Envelope<?> envelope) throws Exception {
        if (envelope instanceof SignalEnvelope) {
            actor.handleSignal(((SignalEnvelope) envelope).getMessage());
        } else {
            actor.onReceive(envelope.getMessage());
        }
    }

    private void handleProcessingError(AbstractActor<?> actor, Exception e) {
        logger.error("Error processing message: {}", currentMessage, e);
        actor.getContext().escalate(e);
    }

    private boolean hasHighPrioritySignals() {
        Envelope<?> peek = messages.peek();
        return peek instanceof SignalEnvelope &&
                ((SignalEnvelope) peek).priority() == SignalPriority.HIGH;
    }

    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    public void suspend() {
        suspended.set(true);
    }

    public void resume() {
        suspended.set(false);
    }

    public void clearMailbox() throws MailboxException {
        if (processingThread.get() != null) {
            logger.warn("Mailbox is being processed, waiting for completion before clearing");
            try {
                // 等待当前处理完成
                while (processingThread.get() != null) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for mailbox processing to complete");
            }
        }

        try {
            // 暂停邮箱
            suspend();

            // 保存当前消息
            Envelope<?> current = currentMessage;
            currentMessage = null;

            // 处理当前正在处理的消息
            if (current != null) {
                handleDeadLetter(current);
            }

            // 批量处理队列中的消息
            List<Envelope<?>> pendingMessages = new ArrayList<>();
            messages.drainTo(pendingMessages);

            if (!pendingMessages.isEmpty()) {
                logger.warn("Converting {} messages to dead letters during mailbox clear",pendingMessages.size());

                for (Envelope<?> envelope : pendingMessages) {
                    try {
                        if (envelope instanceof SignalEnvelope) {
                            // 优先处理系统信号
                            handleSignalDeadLetter((SignalEnvelope) envelope);
                        } else {
                            handleDeadLetter(envelope);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to handle dead letter: {}", envelope, e);
                    }
                }
            }

            // 清空所有队列
            messages.clear();

            logger.info("Mailbox cleared, processed {} dead letters", pendingMessages.size());

        } catch (Exception e) {
            logger.error("Error during mailbox clearing", e);
            throw new MailboxException("Failed to clear mailbox", e);
        } finally {
            // 确保邮箱保持暂停状态
            suspend();
        }
    }

    private void handleSignalDeadLetter(SignalEnvelope envelope) {
        // 特殊处理系统信号
        if (envelope.priority() == SignalPriority.HIGH) {
            logger.error("High priority signal became dead letter: {}", envelope);
        }
        handleDeadLetter(envelope);
    }

    private void handleDeadLetter(Envelope<?> envelope) {
        try {
            IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                    envelope.getMessage(),
                    envelope.getSender().path(),
                    envelope.getRecipient().path(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    envelope.getMessageType(),
                    envelope.getRetryCount(),
                    new HashMap<>()
            );

            // 发送到死信Actor
            if (actorSystem.getDeadLetters() != null) {
                actorSystem.getDeadLetters().tell(deadLetter, ActorRef.noSender());
            } else {
                logger.warn("Dead letter dropped, no dead letter actor available: {}", deadLetter);
            }
        } catch (Exception e) {
            logger.error("Failed to create dead letter from envelope: {}", envelope, e);
        }
    }
}