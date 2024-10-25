package com.avolution.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;

public class BackpressureActor extends AbstractActor {

    private final int backpressureThreshold;
    private int messageCount;

    public BackpressureActor(int backpressureThreshold) {
        this.backpressureThreshold = backpressureThreshold;
        this.messageCount = 0;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Object.class, this::handleMessage)
                .build();
    }

    private void handleMessage(Object message) {
        messageCount++;
        if (messageCount >= backpressureThreshold) {
            applyBackpressure();
        } else {
            processMessage(message);
        }
    }

    private void applyBackpressure() {
        // Implement the logic to apply backpressure
        // For example, you can stop receiving messages temporarily or send a backpressure signal
    }

    private void processMessage(Object message) {
        // Implement the logic to process the message
        // For example, you can send the message to another actor or perform some computation
    }

    public static Props props(int backpressureThreshold) {
        return Props.create(BackpressureActor.class, () -> new BackpressureActor(backpressureThreshold));
    }
}
