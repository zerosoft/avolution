package com.avolution.actor.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ActorContextManager {
    private static final Logger logger = LoggerFactory.getLogger(ActorContextManager.class);

    private final PathNode root;
    private final Map<String, ActorContext> pathToContext;

    // 添加读写锁以确保线程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ActorContextManager() {
        this.root = new PathNode("");
        this.pathToContext = new ConcurrentHashMap<>();
    }

    // 内部类：路径树节点
    private static class PathNode {
        private final String name;
        private final Map<String, PathNode> children = new HashMap<>();
        private ActorContext context;

        PathNode(String name) {
            this.name = name;
        }
    }

    public void addContext(String path, ActorContext context) {
        if (path == null || context == null) {
            throw new IllegalArgumentException("Path and context cannot be null");
        }

        String normalizedPath = normalizePath(path);
        lock.writeLock().lock();
        try {
            // 检查是否已存在
            if (pathToContext.containsKey(normalizedPath)) {
                throw new IllegalStateException("Context already exists for path: " + normalizedPath);
            }

            String[] parts = normalizedPath.split("/");
            PathNode current = root;
            StringBuilder fullPath = new StringBuilder();

            // 创建或更新路径树
            for (String part : parts) {
                if (part.isEmpty()) continue;
                fullPath.append("/").append(part);

                // 检查是否存在中间节点的上下文
                PathNode existingNode = current.children.get(part);
                if (existingNode != null && existingNode.context != null) {
                    logger.warn("Creating child for path {} which has context", fullPath);
                }

                current.children.computeIfAbsent(part, PathNode::new);
                current = current.children.get(part);
            }

            // 设置上下文
            current.context = context;
            pathToContext.put(normalizedPath, context);

            logger.debug("Added context for path: {}", normalizedPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<ActorContext> getContext(String path) {
        return Optional.ofNullable(pathToContext.get(normalizePath(path)));
    }

    public Set<ActorContext> getChildContexts(String parentPath) {
        PathNode node = findNode(normalizePath(parentPath));
        if (node == null) {
            return Collections.emptySet();
        }

        return node.children.values().stream()
                .map(child -> child.context)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void removeContext(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        String normalizedPath = normalizePath(path);
        lock.writeLock().lock();
        try {
            PathNode node = findNode(normalizedPath);
            if (node == null) {
                logger.warn("Attempting to remove non-existent context at path: {}", normalizedPath);
                return;
            }

            // 递归移除所有子节点
            removeNodeRecursively(node, normalizedPath);

            // 从父节点的children中移除当前节点
            PathNode parentNode = findParentNode(normalizedPath);
            if (parentNode != null) {
                String nodeName = getLastPathSegment(normalizedPath);
                parentNode.children.remove(nodeName);
            }

            logger.debug("Removed context and all children at path: {}", normalizedPath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeNodeRecursively(PathNode node, String nodePath) {
        // 先递归处理所有子节点
        for (Map.Entry<String, PathNode> entry : new HashMap<>(node.children).entrySet()) {
            String childPath = nodePath + "/" + entry.getKey();
            removeNodeRecursively(entry.getValue(), childPath);
        }

        // 清理当前节点的上下文
        if (node.context != null) {
            // 从映射中移除
            pathToContext.remove(nodePath);
            // 清空节点上下文
            node.context = null;
        }

        // 清空子节点映射
        node.children.clear();
    }

    private PathNode findParentNode(String path) {
        String parentPath = getParentPath(path);
        return parentPath != null ? findNode(parentPath) : null;
    }

    private String getParentPath(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex > 0 ? path.substring(0, lastSlashIndex) : null;
    }

    private String getLastPathSegment(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }


    private PathNode findNode(String path) {
        String[] parts = path.split("/");
        PathNode current = root;

        for (String part : parts) {
            if (part.isEmpty()) continue;
            current = current.children.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private String normalizePath(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        // 移除末尾的斜杠
        return normalized.endsWith("/") ?
                normalized.substring(0, normalized.length() - 1) : normalized;
    }

    public boolean hasContext(String path) {
        return pathToContext.containsKey(normalizePath(path));
    }

    public int getContextCount() {
        return pathToContext.size();
    }
}