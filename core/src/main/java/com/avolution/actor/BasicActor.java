package com.avolution.actor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicActor implements Actor {
    private final BlockingQueue<Message> messageQueue;
    private final ExecutorService executorService;

    public BasicActor() {
        this.messageQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this::processMessages);
    }

    @Override
    public void receiveMessage(Message message) {
        messageQueue.offer(message);
    }

    @Override
    public void sendMessage(Actor recipient, Message message) {
        recipient.receiveMessage(message);
    }

    private void processMessages() {
        try {
            while (true) {
                Message message = messageQueue.take();
                // Process the message
                System.out.println("Processing message: " + message.getContent());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
