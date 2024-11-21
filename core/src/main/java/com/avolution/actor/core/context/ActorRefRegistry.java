package com.avolution.actor.core.context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.Signal;

public class ActorRefRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ActorRefRegistry.class);

    private final ActorSystem system;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 核心索引映射
    private final Map<String, ActorRef<?>> pathToRef = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> parentToChildren = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> watcherToWatched = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> watchedToWatcher = new ConcurrentHashMap<>();
    private final Map<String, Set<Runnable>> terminationCallbacks = new ConcurrentHashMap<>();

    public ActorRefRegistry(ActorSystem system) {
        this.system = system;
    }

    public void register(ActorRef<?> ref, String parentPath) {
        if (ref == null || ref.path() == null) {
            throw new IllegalArgumentException("Invalid actor reference");
        }

        lock.writeLock().lock();
        try {
            String path = ref.path();

            // 检查是否已存在
            if (pathToRef.containsKey(path)) {
                throw new IllegalStateException("Actor already registered: " + path);
            }

            // 注册主索引
            pathToRef.put(path, ref);

            // 建立父子关系
            if (parentPath != null && !parentPath.isEmpty()) {
                parentToChildren.computeIfAbsent(parentPath, k -> ConcurrentHashMap.newKeySet())
                        .add(path);
            }

            logger.debug("Registered ActorRef: {} under parent: {}", path, parentPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregisterActor(String path) {
        if (path == null) return;

        lock.writeLock().lock();
        try {
            // 获取Actor引用
            ActorRef<?> ref = pathToRef.get(path);
            if (ref == null) {
                logger.warn("Attempting to unregister non-existent actor: {}", path);
                return;
            }

            // 递归停止子Actor
            Set<String> children = parentToChildren.get(path);
            if (children != null) {
                new HashSet<>(children).forEach(childPath ->
                        unregisterActor(childPath));
                parentToChildren.remove(path);
            }

            // 通知监视者
            notifyWatchers(path);

            // 执行终止回调
            executeTerminationCallbacks(path);

            // 清理所有引用
            cleanupReferences(path);

            logger.debug("Unregistered ActorRef: {}", path);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cleanupReferences(String path) {
        try {
            // 1. 清理主索引
            pathToRef.remove(path);

            // 2. 清理父子关系
            parentToChildren.values().forEach(children -> children.remove(path));
            parentToChildren.remove(path);

            // 3. 清理监视关系
            // 3.1 清理作为观察者的记录
            Set<String> watchedActors = watcherToWatched.remove(path);
            if (watchedActors != null) {
                watchedActors.forEach(watchedPath -> {
                    Set<String> watchers = watchedToWatcher.get(watchedPath);
                    if (watchers != null) {
                        watchers.remove(path);
                        // 如果没有观察者了，清理整个集合
                        if (watchers.isEmpty()) {
                            watchedToWatcher.remove(watchedPath);
                        }
                    }
                });
            }

            // 3.2 清理作为被观察者的记录
            Set<String> watchers = watchedToWatcher.remove(path);
            if (watchers != null) {
                watchers.forEach(watcherPath -> {
                    Set<String> watchedPaths = watcherToWatched.get(watcherPath);
                    if (watchedPaths != null) {
                        watchedPaths.remove(path);
                        // 如果没有被观察的Actor了，清理整个集合
                        if (watchedPaths.isEmpty()) {
                            watcherToWatched.remove(watcherPath);
                        }
                    }
                });
            }

            logger.debug("Cleaned up all references for actor: {}", path);
        } catch (Exception e) {
            logger.error("Error cleaning up references for actor: {}", path, e);
            // 确保基本清理完成
            pathToRef.remove(path);
            parentToChildren.remove(path);
        }
    }

    private void notifyWatchers(String path) {
        Set<String> watchers = watchedToWatcher.get(path);
        if (watchers != null) {
            watchers.forEach(watcherPath -> {
                ActorRef<?> watcher = pathToRef.get(watcherPath);
                if (watcher != null) {
                    watcher.tell(Signal.TERMINATED, ActorRef.noSender());
                }
            });
        }
    }

    public void addTerminationCallback(String path, Runnable callback) {
        lock.writeLock().lock();
        try {
            terminationCallbacks.computeIfAbsent(path, k -> ConcurrentHashMap.newKeySet())
                    .add(callback);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void executeTerminationCallbacks(String path) {
        Set<Runnable> callbacks = terminationCallbacks.remove(path);
        if (callbacks != null) {
            callbacks.forEach(callback -> {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.error("Error executing termination callback for actor: {}", path, e);
                }
            });
        }
    }

    public void watch(ActorRef<?> watcher, ActorRef<?> watched) {
        if (!isValidWatchRequest(watcher, watched)) {
            return;
        }

        String watcherPath = watcher.path();
        String watchedPath = watched.path();

        lock.writeLock().lock();
        try {
            if (!pathToRef.containsKey(watchedPath)) {
                watcher.tell(Signal.TERMINATED,ActorRef.noSender());
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

    private boolean isValidWatchRequest(ActorRef<?> watcher, ActorRef<?> watched) {
        return watcher != null && watched != null &&
                watcher.path() != null && watched.path() != null &&
                !watcher.equals(watched);
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
            return new HashSet<>(parentToChildren.getOrDefault(parentPath, Collections.emptySet()));
        } finally {
            lock.readLock().unlock();
        }
    }
}