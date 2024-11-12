package com.avolution.actor.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActorContextManager {
    private static final Logger logger = LoggerFactory.getLogger(ActorContextManager.class);

    // 使用树形结构存储路径关系
    private final PathNode root;
    // 保持直接的路径到上下文的映射，用于快速查找
    private final Map<String, ActorContext> pathToContext;

    public ActorContextManager() {
        this.root = new PathNode("");
        this.pathToContext = new ConcurrentHashMap<>();
    }

    public void terminateAll() {

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
        String[] parts = normalizePath(path).split("/");
        PathNode current = root;

        StringBuilder fullPath = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            fullPath.append("/").append(part);

            current.children.computeIfAbsent(part, PathNode::new);
            current = current.children.get(part);
        }

        current.context = context;
        pathToContext.put(path, context);
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
        String normalizedPath = normalizePath(path);
        PathNode node = findNode(normalizedPath);
        if (node != null) {
            node.context = null;
            // 如果节点没有子节点，可以从树中移除
            if (node.children.isEmpty()) {
                removeNodeFromParent(normalizedPath);
            }
        }
        pathToContext.remove(normalizedPath);
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

    private void removeNodeFromParent(String path) {
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        String nodeName = path.substring(path.lastIndexOf('/') + 1);

        PathNode parentNode = findNode(parentPath);
        if (parentNode != null) {
            parentNode.children.remove(nodeName);
        }
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    public boolean hasContext(String path) {
        return pathToContext.containsKey(normalizePath(path));
    }

    public int getContextCount() {
        return pathToContext.size();
    }
}