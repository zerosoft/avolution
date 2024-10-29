package com.avolution.actor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.StructuredTaskScope;

class ActorRefImpl implements ActorRef {
    private final String path;
    private final StructuredTaskScope.ShutdownOnFailure scope;
    private final BlockingQueue<MessageEnvelope> mailbox;
    private final Actor actor;
    private final ActorContext context;
    private volatile boolean isAlive = true;

    public ActorRefImpl(String path, Actor actor, ActorContext context) {
        this.path = path;
        this.mailbox = new LinkedBlockingQueue<>();
        this.actor = actor;
        this.context = context;
        this.scope = new StructuredTaskScope.ShutdownOnFailure();

        // Start message processing loop using virtual threads
        Thread.startVirtualThread(this::processMessages);
    }

    @Override
    public void tellMessage(Object message, ActorRef sender) {
        if (isAlive) {
            mailbox.offer(new MessageEnvelope(message, sender));
        }
    }

    @Override
    public void forward(Object message, ActorContext context) {
        tellMessage(message, context.sender());
    }

    @Override
    public String path() {
        return path;
    }

    private void processMessages() {
        while (isAlive) {
            try {
                MessageEnvelope envelope = mailbox.take();
                ((ActorContextImpl)context).setSender(envelope.sender());

                // Process message in a new virtual thread
                scope.fork(() -> {
                    try {
                        actor.receive(envelope.message());
                    } catch (Exception e) {
                        // Basic error handling - in practice, would involve supervision strategy
                        System.err.println("Error processing message: " + e.getMessage());
                    }
                    return null;
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void stop() {
        isAlive = false;
        try {
            scope.shutdown();
            scope.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
