package com.avolution.actor.supervision;

import com.avolution.actor.message.SignalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Signal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DeathWatch {
    private static final Logger log = LoggerFactory.getLogger(DeathWatch.class);

    private final ActorSystem system;
    private final Map<ActorRef, Set<ActorRef>> watchedBy;  // 被谁监视
    private final Map<ActorRef, Set<ActorRef>> watching;   // 监视谁
    private final ReentrantReadWriteLock lock;

    public DeathWatch(ActorSystem system) {
        this.system = system;
        this.watchedBy = new ConcurrentHashMap<>();
        this.watching = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void watch(ActorRef watcher, ActorRef watched) {
        if (!isValidWatchRequest(watcher, watched)) {
            return;
        }

        lock.writeLock().lock();
        try {
            if (watched.isTerminated()) {
                notifyWatcher(watcher, createTerminatedSignal(watched, false));
                return;
            }

            watchedBy.computeIfAbsent(watched, k -> ConcurrentHashMap.newKeySet()).add(watcher);
            watching.computeIfAbsent(watcher, k -> ConcurrentHashMap.newKeySet()).add(watched);

            log.debug("Actor {} now watching {}", watcher.path(), watched.path());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unwatch(ActorRef watcher, ActorRef watched) {
        if (!isValidWatchRequest(watcher, watched)) {
            return;
        }

        lock.writeLock().lock();
        try {
            watchedBy.getOrDefault(watched, Collections.emptySet()).remove(watcher);
            watching.getOrDefault(watcher, Collections.emptySet()).remove(watched);

            cleanupEmptySets(watched, watcher);
            log.debug("Actor {} stopped watching {}", watcher.path(), watched.path());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void signalTermination(ActorRef terminated, boolean isPlanned) {
        lock.readLock().lock();
        try {
            Set<ActorRef> watchers = watchedBy.getOrDefault(terminated, Collections.emptySet());
            Signal terminatedSignal = createTerminatedSignal(terminated, isPlanned);

            watchers.forEach(watcher -> notifyWatcher(watcher, terminatedSignal));
            cleanup(terminated);

            log.debug("Notified {} watchers about termination of {}",
                    watchers.size(), terminated.path());
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean isValidWatchRequest(ActorRef watcher, ActorRef watched) {
        return watcher != null && watched != null && watcher != watched;
    }

    private Signal createTerminatedSignal(ActorRef terminated, boolean isPlanned) {
        return Signal.STOP;
    }

    private void notifyWatcher(ActorRef watcher, Signal signal) {
        try {
            watcher.tell(signal, ActorRef.noSender());
        } catch (Exception e) {
            log.error("Failed to notify watcher {} about termination", watcher.path(), e);
        }
    }

    private void cleanup(ActorRef actor) {
        lock.writeLock().lock();
        try {
            watchedBy.remove(actor);
            watching.remove(actor);

            // 清理所有对该actor的引用
            watchedBy.values().forEach(set -> set.remove(actor));
            watching.values().forEach(set -> set.remove(actor));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cleanupEmptySets(ActorRef watched, ActorRef watcher) {
        if (watchedBy.get(watched) != null && watchedBy.get(watched).isEmpty()) {
            watchedBy.remove(watched);
        }
        if (watching.get(watcher) != null && watching.get(watcher).isEmpty()) {
            watching.remove(watcher);
        }
    }
}