package com.avolution.actor.supervision;



/**
 * Actor死亡监视器
 */
import com.avolution.actor.message.Terminated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeathWatch {
    private static final Logger logger = LoggerFactory.getLogger(DeathWatch.class);

    private final ActorSystem system;
    private final Map<ActorRef, Set<ActorRef>> watchedBy;
    private final Map<ActorRef, Set<ActorRef>> watching;

    public DeathWatch(ActorSystem system) {
        this.system = system;
        this.watchedBy = new ConcurrentHashMap<>();
        this.watching = new ConcurrentHashMap<>();
    }

    public synchronized void watch(ActorRef watcher, ActorRef watched) {
        if (watcher == null || watched == null || watcher == watched) {
            return;
        }

        if (watched.isTerminated()) {
            notifyWatcher(watcher, new Terminated(watched.path(), "Actor already terminated", false));
            return;
        }

        watchedBy.computeIfAbsent(watched, k -> ConcurrentHashMap.newKeySet()).add(watcher);
        watching.computeIfAbsent(watcher, k -> ConcurrentHashMap.newKeySet()).add(watched);

        logger.debug("Actor {} now watching {}", watcher.path(), watched.path());
    }

    public synchronized void unwatch(ActorRef watcher, ActorRef watched) {
        if (watcher == null || watched == null) {
            return;
        }

        Optional.ofNullable(watchedBy.get(watched))
                .ifPresent(watchers -> {
                    watchers.remove(watcher);
                    if (watchers.isEmpty()) {
                        watchedBy.remove(watched);
                    }
                });

        Optional.ofNullable(watching.get(watcher))
                .ifPresent(watchedActors -> {
                    watchedActors.remove(watched);
                    if (watchedActors.isEmpty()) {
                        watching.remove(watcher);
                    }
                });
    }

    public void terminated(ActorRef actor) {
        if (actor == null) return;

        Set<ActorRef> watchers = watchedBy.remove(actor);
        if (watchers != null) {
            Terminated terminated = new Terminated(actor.path(), "Normal termination", true);
            watchers.forEach(watcher -> notifyWatcher(watcher, terminated));
        }

        Set<ActorRef> watchedActors = watching.remove(actor);
        if (watchedActors != null) {
            watchedActors.forEach(watched ->
                    Optional.ofNullable(watchedBy.get(watched))
                            .ifPresent(ws -> ws.remove(actor))
            );
        }

        logger.info("Actor {} terminated, notified {} watchers",
                actor.path(), watchers != null ? watchers.size() : 0);
    }

    private void notifyWatcher(ActorRef watcher, Terminated terminated) {
        try {
            watcher.tell(terminated, ActorRef.noSender());
        } catch (Exception e) {
            logger.error("Failed to notify watcher {} about termination of {}",
                    watcher.path(), terminated.getActorPath(), e);
        }
    }
}