package com.avolution.actor.mailbox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Priority;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;

/**
 * Actor消息邮箱实现
 */
public class Mailbox {
    private static final Logger logger = LoggerFactory.getLogger(Mailbox.class);

    public static final int TIMEOUT = 100;

    private final PriorityBlockingQueue<Envelope> messages;

    private final AtomicBoolean suspended = new AtomicBoolean(false);

    private final ActorSystem actorSystem;

    public Mailbox(ActorSystem actorSystem,int capacity) {
        this.actorSystem=actorSystem;
        this.messages = new PriorityBlockingQueue<>(capacity, this::compareEnvelopes);
    }

    /**
     * 优先级 SIGNAL 高于 SYSTEM 高于 NORMAL
     * 同级别比较 Priority HIGH 优先于 NORMAL 优先于 LOW
     * @param e1
     * @param e2
     * @return
     */
    private int compareEnvelopes(Envelope e1, Envelope e2) {
        // 首先比较消息类型优先级
        int messageTypeComparison = compareMessageType(e1.getMessageType(), e2.getMessageType());
        if (messageTypeComparison != 0) {
            return messageTypeComparison;
        } else {
            // 如果消息类型优先级相同，则比较优先级
            return e1.getPriority().compareTo(e2.getPriority());
        }
    }

    private int compareMessageType(MessageType type1, MessageType type2) {
        // 直接使用 MessageType 中定义的优先级值进行比较
        // 注意：优先级值越小，优先级越高，所以需要反转比较结果
        return Integer.compare(type2.getPriority(), type1.getPriority());
    }

    public void enqueue(Envelope envelope) {
        if (suspended.get() && !(isHighPrioritySignal(envelope))) {
            return;
        }
        offerMessage(envelope);
    }

    private boolean isHighPrioritySignal(Envelope envelope) {
        return envelope.getMessageType().equals(MessageType.SIGNAL)
                && envelope.getPriority() == Priority.HIGH;
    }

    private void offerMessage(Envelope envelope) {
        if (!messages.offer(envelope, TIMEOUT, TimeUnit.MILLISECONDS)) {
            handleEnqueueTimeout(envelope);
        }
    }

    private void handleEnqueueTimeout(Envelope envelope) {
        logger.warn("Failed to enqueue message due to timeout: {}", envelope);
        convertToDeadLetter(envelope, "Enqueue timeout");
    }

    public Envelope poll() {
        return messages.poll();
    }

    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    public boolean hasHighPrioritySignals() {
        Envelope peek = messages.peek();
        if (peek==null){
            return false;
        }
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