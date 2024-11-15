package com.avolution.actor.mailbox;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.SignalEnvelope;
import com.avolution.actor.message.SignalPriority;
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

    public Mailbox(int capacity) {
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

    public void process(AbstractActor<?> actor) {
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

    private void processMessage(AbstractActor<?> actor, Envelope<?> envelope) throws Exception {
        if (envelope instanceof SignalEnvelope) {
            actor.handleSignal(((SignalEnvelope) envelope).getMessage());
        } else {
            actor.handle((Envelope) envelope);
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
}