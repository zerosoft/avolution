package com.avolution.actor.core.context;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.avolution.actor.message.Signal;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
/**
 * Actor引用注册表
 * 负责管理所有Actor的引用、层级关系和监视关系
 *
 * 主要功能：
 * 1. Actor引用管理 - 注册和注销Actor引用
 *
 * 2. 层级关系管理 - 维护Actor的父子关系
 *
 * 3. 监视关系管理 - 处理Actor之间的监视关系
 *
 * 4. 缓存优化 - 提供高性能的Actor引用查询
 *
 */
public class ActorRefRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ActorRefRegistry.class);
    private final ActorSystem system;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 核心数据结构
    // 使用ConcurrentSkipListMap存储Actor路径到引用的映射，支持高并发
    private final ConcurrentSkipListMap<String, ActorRef<?>> pathToRef = new ConcurrentSkipListMap<>();
    // 存储Actor的父子关系
    private final ConcurrentHashMap<String, Set<String>> parentToChildren = new ConcurrentHashMap<>();
    // 存储监视关系：谁在监视谁
    private final ConcurrentHashMap<String, Set<String>> watcherToWatched = new ConcurrentHashMap<>();
    // 存储监视关系：被谁监视
    private final ConcurrentHashMap<String, Set<String>> watchedToWatcher = new ConcurrentHashMap<>();

    // Actor引用缓存，提升查询性能
    private final LoadingCache<String, ActorRef<?>> refCache;

    public ActorRefRegistry(ActorSystem system) {
        this.system = system;
        this.refCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, ActorRef<?>>() {
                    @Override
                    public ActorRef<?> load(String path) {
                        return pathToRef.get(path);
                    }
                });
    }

    // 1. Actor 注册
    public void register(ActorRef<?> ref, String parentPath) {
        lock.writeLock().lock();
        try {
            String path = ref.path();
            // 检查是否已经注册
            if (pathToRef.containsKey(path)) {
                logger.warn("Actor already registered: {}", path);
                return;
            }

            // 注册Actor引用
            pathToRef.put(path, ref);

            // 建立父子关系
            if (parentPath != null) {
                Set<String> children = parentToChildren.computeIfAbsent(parentPath, k -> ConcurrentHashMap.newKeySet());
                children.add(path);
                logger.debug("Established parent-child relationship: {} -> {}", parentPath, path);
            }

            // 刷新缓存
            refCache.put(path, ref);
            logger.debug("Registered actor: {} with parent: {}", path, parentPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 2. Actor 注销
    public void unregister(String path) {
        lock.writeLock().lock();
        try {
            // 移除引用
            ActorRef<?> ref = pathToRef.remove(path);
            if (ref == null) return;

            // 清理父子关系
            String parentPath = getParentPath(path);
            if (parentPath != null) {
                Set<String> siblings = parentToChildren.get(parentPath);
                if (siblings != null) {
                    siblings.remove(path);
                    if (siblings.isEmpty()) {
                        parentToChildren.remove(parentPath);
                    }
                }
            }

            // 清理监视关系
            clearWatchRelations(path);

            // 清理缓存
            refCache.invalidate(path);
            logger.debug("Unregistered actor: {}", path);
        } finally {
            lock.writeLock().unlock();
        }
    }


    // 4. 查询方法
    public ActorRef<?> getRef(String path) {
        try {
            return refCache.get(path);
        } catch (Exception e) {
            logger.error("Failed to get actor ref: {}", path, e);
            return null;
        }
    }

    /**
     * 获取监视者路径集合
     * @param path 路径
     * @return 监视者路径集合
     */
    public Set<String> getWatchers(String path) {
        lock.readLock().lock();
        try {
            return watchedToWatcher.getOrDefault(path, Collections.emptySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 批量注册Actor引用
     * @param refs 引用集合
     * @param parentPath 父路径
     */
    public void registerAll(Map<String, ActorRef<?>> refs, String parentPath) {
        lock.writeLock().lock();
        try {
            // 批量注册Actor引用
            refs.forEach((path, ref) -> {
                // 注册Actor引用
                pathToRef.put(path, ref);
                // 建立父子关系
                if (parentPath != null) {
                    parentToChildren.computeIfAbsent(parentPath, k -> ConcurrentHashMap.newKeySet())
                            .add(path);
                }
                // 刷新缓存
                refCache.put(path, ref);
            });
            logger.debug("Batch registered {} actors", refs.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    /**
     * 批量注销Actor引用
     * @param paths 路径集合
     */
    public void unregisterAll(Set<String> paths) {
        lock.writeLock().lock();
        try {
            // 批量注销Actor引用
            paths.forEach(this::unregister);
            logger.debug("Batch unregistered {} actors", paths.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 异步通知监视者`
     * @param path 路径
     */
    private void notifyWatchersAsync(String path) {
        system.getScheduler().execute(() -> {
            getWatchers(path).forEach(watcherPath -> {
                ActorRef<?> watcher = getRef(watcherPath);
                if (watcher != null) {
                    watcher.tell(Signal.TERMINATED, ActorRef.noSender());
                }
            });
        });
    }
    /**
     * 获取父路径
     * @param path 路径
     * @return 父路径
     */
    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : null;
    }
    /**
     * 清理监视关系
     * @param path 路径
     */
    private void clearWatchRelations(String path) {
        lock.writeLock().lock();
        try {
            // 1. 清理作为观察者的关系
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

            // 2. 清理作为被观察者的关系
            Set<String> watchers = watchedToWatcher.remove(path);
            if (watchers != null) {
                watchers.forEach(watcherPath -> {
                    Set<String> watchedSet = watcherToWatched.get(watcherPath);
                    if (watchedSet != null) {
                        watchedSet.remove(path);
                        if (watchedSet.isEmpty()) {
                            watcherToWatched.remove(watcherPath);
                        }
                    }
                });
            }

            // 异步通知观察者
            notifyWatchersAsync(path);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
