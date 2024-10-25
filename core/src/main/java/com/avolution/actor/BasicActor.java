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
                handleMessage(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleMessage(Message message) {
        // Custom message handling logic
        System.out.println("Processing message: " + message.getContent());
        // Add more custom handling logic here
    }

    public void restart() {
        // Implement restart logic
        System.out.println("Restarting actor");
    }

    public void stop() {
        // Implement stop logic
        System.out.println("Stopping actor");
    }

    public void resume() {
        // Implement resume logic
        System.out.println("Resuming actor");
    }
}
