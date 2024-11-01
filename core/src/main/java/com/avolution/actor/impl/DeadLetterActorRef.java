package com.avolution.actor.impl;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.DeadLetter;
import com.avolution.actor.message.Message;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DeadLetterActorRef implements ActorRef {

    public static final DeadLetterActorRef INSTANCE = new DeadLetterActorRef();
    
    private final List<Consumer<DeadLetter>> subscribers = new CopyOnWriteArrayList<>();
    private static final String DEAD_LETTERS_PATH = "deadLetters";

    private DeadLetterActorRef() {
        // 默认添加日志订阅者
        subscribe(deadLetter -> 
            System.err.println("DeadLetter: " + formatDeadLetter(deadLetter))
        );
    }

    @Override
    public void tellMessage(Object message, ActorRef sender) {
        DeadLetter deadLetter = new DeadLetter(message, sender, this);
        notifySubscribers(deadLetter);
    }

    @Override
    public void tell(Message<?> message, ActorRef sender) {

    }

    @Override
    public <T> CompletionStage<T> ask(Message<?> message, Duration timeout) {
        return null;
    }

    @Override
    public String path() {
        return DEAD_LETTERS_PATH;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void stop() {
        // Dead letters actor cannot be stopped
    }

    /**
     * 订阅死信消息
     */
    public void subscribe(Consumer<DeadLetter> subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(Consumer<DeadLetter> subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * 通知所有订阅者
     */
    private void notifySubscribers(DeadLetter deadLetter) {
        subscribers.forEach(subscriber -> {
            try {
                subscriber.accept(deadLetter);
            } catch (Exception e) {
                System.err.println("Error in dead letter subscriber: " + e.getMessage());
            }
        });
    }

    private String formatDeadLetter(DeadLetter deadLetter) {
        return String.format(
            "Message of type [%s] from [%s] to [%s] was not delivered. %s",
            deadLetter.message().getClass().getName(),
            deadLetter.sender() != null ? deadLetter.sender().path() : "NoSender",
            deadLetter.recipient() != null ? deadLetter.recipient().path() : "Unknown",
            deadLetter.message()
        );
    }
} 