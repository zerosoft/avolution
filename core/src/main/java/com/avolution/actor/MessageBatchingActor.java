package com.avolution.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageBatchingActor extends AbstractActor {

    private final List<Object> messageBatch = new ArrayList<>();
    private final int batchSize;
    private final long batchTimeout;
    private long lastBatchTime;

    public MessageBatchingActor(int batchSize, long batchTimeout) {
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        this.lastBatchTime = System.currentTimeMillis();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Object.class, this::handleMessage)
                .matchEquals("flush", msg -> flushBatch())
                .build();
    }

    private void handleMessage(Object message) {
        messageBatch.add(message);
        if (messageBatch.size() >= batchSize || isTimeoutExceeded()) {
            flushBatch();
        }
    }

    private boolean isTimeoutExceeded() {
        return System.currentTimeMillis() - lastBatchTime >= batchTimeout;
    }

    private void flushBatch() {
        if (!messageBatch.isEmpty()) {
            // Process the batch of messages
            processBatch(new ArrayList<>(messageBatch));
            messageBatch.clear();
            lastBatchTime = System.currentTimeMillis();
        }
    }

    private void processBatch(List<Object> batch) {
        // Implement the logic to process the batch of messages
        // For example, you can send the batch to another actor or perform some computation
    }

    public static Props props(int batchSize, long batchTimeout) {
        return Props.create(MessageBatchingActor.class, () -> new MessageBatchingActor(batchSize, batchTimeout));
    }
}
