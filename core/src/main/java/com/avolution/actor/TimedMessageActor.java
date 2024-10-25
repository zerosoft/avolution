package com.avolution.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class TimedMessageActor extends AbstractActor {

    private final long messageInterval;
    private final long messageTimeout;

    public TimedMessageActor(long messageInterval, long messageTimeout) {
        this.messageInterval = messageInterval;
        this.messageTimeout = messageTimeout;
    }

    @Override
    public void preStart() {
        getContext().getSystem().scheduler().schedule(
                Duration.Zero(),
                Duration.create(messageInterval, TimeUnit.MILLISECONDS),
                getSelf(),
                "sendMessage",
                getContext().getSystem().dispatcher(),
                null
        );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("sendMessage", msg -> sendMessage())
                .match(Object.class, this::handleMessage)
                .build();
    }

    private void sendMessage() {
        // Implement the logic to send a message
        // For example, you can send a message to another actor or perform some computation
    }

    private void handleMessage(Object message) {
        // Implement the logic to handle received messages
        // For example, you can process the message or forward it to another actor
    }

    public static Props props(long messageInterval, long messageTimeout) {
        return Props.create(TimedMessageActor.class, () -> new TimedMessageActor(messageInterval, messageTimeout));
    }
}
