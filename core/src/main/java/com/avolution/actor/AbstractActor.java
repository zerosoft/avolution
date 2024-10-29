package com.avolution.actor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractActor implements Actor {
    private final BlockingQueue<Message> messageQueue;
    private final ExecutorService executorService;
    private final List<AbstractActor> children;
    private AbstractActor parent;
    private String id;

    public AbstractActor(String id) {
        this.messageQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this::processMessages);
        this.children = new ArrayList<>();
        this.id = id;
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
                handleMessage(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void handleMessage(Message message);

    public void restart() {
        System.out.println("Restarting actor");
    }

    public void stop() {
        System.out.println("Stopping actor");
    }

    public void resume() {
        System.out.println("Resuming actor");
    }

    public void addChild(AbstractActor child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(AbstractActor child) {
        children.remove(child);
        child.setParent(null);
    }

    public List<AbstractActor> getChildren() {
        return children;
    }

    public AbstractActor getParent() {
        return parent;
    }

    public void setParent(AbstractActor parent) {
        this.parent = parent;
    }

    public void propagateError(Throwable cause) {
        if (parent != null) {
            parent.handleError(this, cause);
        }
    }

    protected void handleError(AbstractActor child, Throwable cause) {
        System.out.println("Handling error from child: " + child);
        propagateError(cause);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void monitorHealth() {
        // Implement health monitoring logic here
    }

    public void handleFailure(Throwable cause) {
        // Implement failure handling logic here
    }
}
