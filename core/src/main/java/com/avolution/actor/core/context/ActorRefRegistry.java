package com.avolution.actor.core.context;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.Terminated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ActorRefRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ActorRefRegistry.class);
    // Actor系统引用
    private final ActorSystem system;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, ActorRef<?>> pathToRef = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> parentToChildren = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> watcherToWatched = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> watchedToWatcher = new ConcurrentHashMap<>();

    public ActorRefRegistry(ActorSystem system) {
        this.system = system;
    }

    public void register(ActorRef<?> ref, String parentPath) {
        if (ref == null) {
            throw new IllegalArgumentException("ActorRef cannot be null");
        }

        lock.writeLock().lock();
        try {
            String path = ref.path();
            if (pathToRef.containsKey(path)) {
                throw new IllegalStateException("Actor already registered at path: " + path);
            }

            pathToRef.put(path, ref);

            if (parentPath != null) {
                parentToChildren.computeIfAbsent(parentPath, k -> ConcurrentHashMap.newKeySet())
                        .add(path);
            }

            logger.debug("Registered ActorRef: {}", path);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregister(String path, String reason) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        lock.writeLock().lock();
        try {
            // 递归终止子Actor
            Set<String> children = parentToChildren.get(path);
            if (children != null) {
                new HashSet<>(children).forEach(childPath -> unregister(childPath, "Parent stopped"));
                parentToChildren.remove(path);
            }

            // 通知监视者
            notifyWatchers(path, reason);

            // 清理所有相关索引
            cleanupReferences(path);

            logger.debug("Unregistered ActorRef: {}", path);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cleanupReferences(String path) {
        // 清理父子关系
        parentToChildren.values().forEach(childSet -> childSet.remove(path));

        // 清理监视关系
        Set<String> watched = watcherToWatched.remove(path);
        if (watched != null) {
            watched.forEach(watchedPath -> {
                Set<String> watchers = watchedToWatcher.get(watchedPath);
                if (watchers != null) {
                    watchers.remove(path);
                    if (watchers.isEmpty()) {
                        watchedToWatcher.remove(watchedPath);
                    }
                }
            });
        }

        // 移除主索引
        pathToRef.remove(path);
    }

    public Optional<ActorRef<?>> getRef(String path) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(pathToRef.get(path));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<String> getChildren(String parentPath) {
        lock.readLock().lock();
        try {
            return parentToChildren.getOrDefault(parentPath, Collections.emptySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void watch(ActorRef<?> watcher, ActorRef<?> watched) {
        if (watcher == null || watched == null || watcher.equals(watched)) {
            throw new IllegalArgumentException("Invalid watch parameters");
        }

        String watcherPath = watcher.path();
        String watchedPath = watched.path();

        lock.writeLock().lock();
        try {
            if (!pathToRef.containsKey(watchedPath)) {
                // 如果目标已经终止，直接发送终止通知
                watcher.tell(new Terminated(watchedPath, "Actor already terminated", false), ActorRef.noSender());
                return;
            }

            watcherToWatched.computeIfAbsent(watcherPath, k -> ConcurrentHashMap.newKeySet())
                    .add(watchedPath);
            watchedToWatcher.computeIfAbsent(watchedPath, k -> ConcurrentHashMap.newKeySet())
                    .add(watcherPath);

            logger.debug("Actor {} now watching {}", watcherPath, watchedPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unwatch(ActorRef<?> watcher, ActorRef<?> watched) {
        String watcherPath = watcher.path();
        String watchedPath = watched.path();

        lock.writeLock().lock();
        try {
            Set<String> watchedSet = watcherToWatched.get(watcherPath);
            if (watchedSet != null) {
                watchedSet.remove(watchedPath);
                if (watchedSet.isEmpty()) {
                    watcherToWatched.remove(watcherPath);
                }
            }

            Set<String> watcherSet = watchedToWatcher.get(watchedPath);
            if (watcherSet != null) {
                watcherSet.remove(watcherPath);
                if (watcherSet.isEmpty()) {
                    watchedToWatcher.remove(watchedPath);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void notifyWatchers(String terminatedPath, String reason) {
        Set<String> watchers = watchedToWatcher.get(terminatedPath);
        if (watchers != null) {
            for (String watcherPath : new HashSet<>(watchers)) {
                ActorRef<?> watcherRef = pathToRef.get(watcherPath);
                if (watcherRef != null) {
                    watcherRef.tell(new Terminated(terminatedPath, reason, false), ActorRef.noSender());
                }
            }
            watchedToWatcher.remove(terminatedPath);
        }
    }

    public int getRefCount() {
        lock.readLock().lock();
        try {
            return pathToRef.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(String path) {
        lock.readLock().lock();
        try {
            return pathToRef.containsKey(path);
        } finally {
            lock.readLock().unlock();
        }
    }
}