package com.avolution.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Actor的实现
 */
class ActorRefImpl implements ActorRef {

    private Logger logger= LoggerFactory.getLogger(ActorRefImpl.class);

    private final String path;

    private final BlockingQueue<MessageEnvelope> mailbox;

    private final Actor actor;

    private final ActorContext context;

    private volatile boolean isAlive = true;

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    public ActorRefImpl(String path, Actor actor, ActorContext context) {
        this.path = path;
        this.mailbox = new LinkedBlockingQueue<>();
        this.actor = actor;
        this.context = context;

        Thread.ofVirtual().name("Actor://"+path).start(this::processMessages);
        // Start message processing loop using virtual threads
//        Thread.startVirtualThread(this::processMessages);
    }

    @Override
    public void tellMessage(Object message, ActorRef sender) {
        if (isAlive) {
            boolean offer = mailbox.offer(new MessageEnvelope(message, sender));
            if (offer){
                wakeUpProcessor(); // Notify the message processor
            }else {
                logger.error("Mailbox is full, message dropped");
            }

        }
    }

    private void wakeUpProcessor() {
        lock.lock();
        try {
            condition.signal(); // Notify the message processor
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void forward(Object message, ActorContext context) {
        tellMessage(message, context.getSender());
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public ActorRef createChild(Props props, String name) {
        return  context.actorOf(props, name);
    }

    private void processMessages() {
        while (isAlive) {
            try {
                // 使用 take() 方法阻塞，直到有新消息
                MessageEnvelope envelope = mailbox.take();
                // 处理消息
                try {
                    actor.context().setSender(envelope.sender());
                    actor.receive(envelope.message());
                    logger.info("Message processed: " + envelope.message());
                } catch (Exception e) {
                  logger.error("Error processing message: " + e.getMessage());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleError(Exception e) {
        // Implement a more robust error handling mechanism
        System.err.println("Error processing message: " + e.getMessage());
        // Consider logging, retries, or other mechanisms here
    }


    void stop() {
        isAlive = false;

    }
}
