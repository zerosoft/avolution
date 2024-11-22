package com.avolution.actor.stream;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EventStream {
    private static final Logger logger = LoggerFactory.getLogger(EventStream.class);

    private final ActorSystem system;
    private final Map<Class<?>, Set<ActorRef>> subscribers = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public EventStream(ActorSystem system) {
        this.system = system;
    }

    public <T> void subscribe(Class<T> eventType, ActorRef<?> subscriber) {
        lock.writeLock().lock();
        try {
            subscribers.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet())
                    .add(subscriber);
            logger.debug("Actor {} subscribed to events of type {}",
                    subscriber.path(), eventType.getSimpleName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> void unsubscribe(Class<T> eventType, ActorRef subscriber) {
        lock.writeLock().lock();
        try {
            Set<ActorRef> eventSubscribers = subscribers.get(eventType);
            if (eventSubscribers != null) {
                eventSubscribers.remove(subscriber);
                logger.debug("Actor {} unsubscribed from events of type {}",
                        subscriber.path(), eventType.getSimpleName());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void publish(Object event) {
        if (event == null) return;

        lock.readLock().lock();
        try {
            Set<ActorRef> eventSubscribers = subscribers.get(event.getClass());
            if (eventSubscribers != null && !eventSubscribers.isEmpty()) {
                EventEnvelope envelope = EventEnvelope.builder()
                        .event(event)
                        .build();

                for (ActorRef subscriber : eventSubscribers) {
                    try {
                        subscriber.tell(envelope, ActorRef.noSender());
                    } catch (Exception e) {
                        logger.error("Failed to deliver event {} to actor {}",
                                event.getClass().getSimpleName(), subscriber.path(), e);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}