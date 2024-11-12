package com.avolution.actor.context;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ActorRefRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ActorRefRegistry.class);

    private final ActorSystem system;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 主索引：路径到ActorRef的映射
    private final Map<String, ActorRef<?>> pathToRef = new ConcurrentHashMap<>();

    // 父子关系索引
    private final Map<String, Set<String>> parentToChildren = new ConcurrentHashMap<>();

    // 监视关系索引
    private final Map<String, Set<String>> watcherToWatched = new ConcurrentHashMap<>();

    public ActorRefRegistry(ActorSystem system) {
        this.system = system;
    }

    public void register(ActorRef<?> ref, String parentPath) {
        lock.writeLock().lock();
        try {
            String path = ref.path();
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

    public void unregister(String path) {
        lock.writeLock().lock();
        try {
            // 1. 移除主索引
            pathToRef.remove(path);

            // 2. 清理父子关系
            parentToChildren.values().forEach(children -> children.remove(path));
            Set<String> children = parentToChildren.remove(path);
            if (children != null) {
                children.forEach(this::unregister);
            }

            // 3. 清理监视关系
            watcherToWatched.values().forEach(watched -> watched.remove(path));
            watcherToWatched.remove(path);

            logger.debug("Unregistered ActorRef: {}", path);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<ActorRef<?>> getRef(String path) {
        lock.readLock().lock();
        try {
            ActorRef<?> ref = pathToRef.get(path);
            return ref != null ? Optional.ofNullable(ref) : Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<String> getChildren(String parentPath) {
        lock.readLock().lock();
        try {
            return new HashSet<>(parentToChildren.getOrDefault(parentPath, Collections.emptySet()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addWatch(String watcherPath, String watchedPath) {
        lock.writeLock().lock();
        try {
            watcherToWatched.computeIfAbsent(watcherPath, k -> ConcurrentHashMap.newKeySet())
                    .add(watchedPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

}