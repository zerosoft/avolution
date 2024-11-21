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

    // 监视关系映射
    private final Map<ActorRef<?>, Set<WatchRegistration>> watchedBy;  // 被谁监视
    private final Map<ActorRef<?>, Set<WatchRegistration>> watching;   // 监视谁
    private final ReentrantReadWriteLock lock;

    public record WatchRegistration(
            ActorRef<?> watcher,
            ActorRef<?> watched,
            DeathWatchCallback callback
    ) {}

    @FunctionalInterface
    public interface DeathWatchCallback {
        void onTerminated(ActorRef<?> actor, boolean normal);
    }

    public DeathWatch(ActorSystem system) {
        this.system = system;
        this.watchedBy = new ConcurrentHashMap<>();
        this.watching = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void watch(ActorRef<?> watcher, ActorRef<?> watched, DeathWatchCallback callback) {
        if (!isValidWatchRequest(watcher, watched)) {
            return;
        }

        WatchRegistration registration = new WatchRegistration(watcher, watched, callback);
        lock.writeLock().lock();
        try {
            watchedBy.computeIfAbsent(watched, k -> ConcurrentHashMap.newKeySet()).add(registration);
            watching.computeIfAbsent(watcher, k -> ConcurrentHashMap.newKeySet()).add(registration);

            // 如果目标已终止，立即触发回调
            if (watched.isTerminated()) {
                unwatch(watcher, watched);
                callback.onTerminated(watched, true);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unwatch(ActorRef<?> watcher, ActorRef<?> watched) {
        lock.writeLock().lock();
        try {
            Set<WatchRegistration> watcherRegs = watching.get(watcher);
            Set<WatchRegistration> watchedRegs = watchedBy.get(watched);

            if (watcherRegs != null) {
                watcherRegs.removeIf(reg -> reg.watched().equals(watched));
                if (watcherRegs.isEmpty()) {
                    watching.remove(watcher);
                }
            }

            if (watchedRegs != null) {
                watchedRegs.removeIf(reg -> reg.watcher().equals(watcher));
                if (watchedRegs.isEmpty()) {
                    watchedBy.remove(watched);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void signalTermination(ActorRef<?> actor, boolean normal) {
        Set<WatchRegistration> registrations = watchedBy.remove(actor);
        if (registrations != null) {
            registrations.forEach(reg -> {
                try {
                    reg.callback().onTerminated(actor, normal);
                    watching.computeIfPresent(reg.watcher(), (k, v) -> {
                        v.remove(reg);
                        return v.isEmpty() ? null : v;
                    });
                } catch (Exception e) {
                    log.error("Error executing death watch callback for {}", actor.path(), e);
                }
            });
        }
    }

    private boolean isValidWatchRequest(ActorRef<?> watcher, ActorRef<?> watched) {
        return watcher != null && watched != null && !watcher.equals(watched);
    }
}