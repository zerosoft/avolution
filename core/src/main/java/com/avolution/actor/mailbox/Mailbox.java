package com.avolution.actor.mailbox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.SignalEnvelope;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor消息邮箱实现
 */
public class Mailbox {
    private static final Logger logger = LoggerFactory.getLogger(Mailbox.class);

    private final PriorityBlockingQueue<Envelope> messages;
    private final AtomicBoolean suspended = new AtomicBoolean(false);

    private final ActorSystem actorSystem;

    public Mailbox(ActorSystem actorSystem,int capacity) {
        this.actorSystem=actorSystem;
        this.messages = new PriorityBlockingQueue<>(capacity, this::compareEnvelopes);
    }

    private int compareEnvelopes(Envelope e1, Envelope e2) {
        if (e1 instanceof SignalEnvelope && e2 instanceof SignalEnvelope) {
            return (e1).getPriority().compareTo((e2).getPriority());
        }
        return e1.getPriority().compareTo(e2.getPriority());
    }

    public void enqueue(Envelope envelope) {
        if (suspended.get() && !(isHighPrioritySignal(envelope))) {
            return;
        }
        offerMessage(envelope);
    }

    private boolean isHighPrioritySignal(Envelope envelope) {
        return envelope instanceof SignalEnvelope && 
                envelope.getPriority() == Envelope.Priority.HIGH;
    }

    private void offerMessage(Envelope envelope) {
        if (!messages.offer(envelope, 100, TimeUnit.MILLISECONDS)) {
            handleEnqueueTimeout(envelope);
        }
    }

    private void handleEnqueueTimeout(Envelope envelope) {
        logger.warn("Failed to enqueue message due to timeout: {}", envelope);
        convertToDeadLetter(envelope, "Enqueue timeout");
    }

    private void handleEnqueueError(Envelope envelope, Exception e) {
        logger.error("Error enqueueing message: {}", envelope, e);
        convertToDeadLetter(envelope, "Enqueue error: " + e.getMessage());
    }

    public Envelope poll() {
        Envelope envelope = messages.poll();
        return envelope;
    }

    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    public boolean hasHighPrioritySignals() {
        Envelope peek = messages.peek();
        return isHighPrioritySignal(peek);
    }

    public void suspend() {
        suspended.set(true);
    }

    public void resume() {
        suspended.set(false);
    }

    public boolean isSuspended() {
        return suspended.get();
    }

    public void clear() {
        suspend();
        drainAndHandleMessages();
        messages.clear();
    }

    private void drainAndHandleMessages() {
        List<Envelope> pendingMessages = new ArrayList<>();
        messages.drainTo(pendingMessages);
        
        for (Envelope envelope : pendingMessages) {
            convertToDeadLetter(envelope, "Mailbox cleared");
        }
    }

    private void convertToDeadLetter(Envelope envelope, String reason) {
        try {
            IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                envelope.getMessage(),
                envelope.getSender().path(),
                envelope.getRecipient().path(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                envelope.getMessageType(),
                envelope.getRetryCount(),
                Map.of("reason", reason)
            );
            actorSystem.getDeadLetters().tell(deadLetter, ActorRef.noSender());
        } catch (Exception e) {
            logger.error("Failed to create dead letter from envelope: {}", envelope, e);
        }
    }
}