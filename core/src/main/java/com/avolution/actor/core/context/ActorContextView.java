package com.avolution.actor.core.context;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;

/**
 * Actor上下文的只读视图
 * 提供对ActorContext的受限访问
 */
public class ActorContextView {
    private final ActorContext context;

    public ActorContextView(ActorContext context) {
        this.context = context;
    }

    /**
     * 获取Actor路径
     */
    public String getPath() {
        return context.getPath();
    }

    /**
     * 获取Actor系统
     */
    public ActorSystem getSystem() {
        return context.getActorSystem();
    }

    /**
     * 获取父Actor引用
     */
    public Optional<ActorRef<Object>> getParent() {
        ActorContext parent = context.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        // 确保返回父Actor的引用
        return Optional.of((ActorRef<Object>) parent.getUnTypedActor().getSelfRef());
    }

    /**
     * 获取子Actor引用集合
     */
    public Set<ActorRef<Object>> getChildren() {
        Set<ActorRef<Object>> children = new HashSet<>();
        context.getChildrenView().values().forEach(ref -> children.add((ActorRef<Object>) ref));
        return children;
    }

    /**
     * 查找指定名称的子Actor
     */
    public Optional<ActorRef<Object>> findChild(String name) {
        return Optional.ofNullable((ActorRef<Object>) context.getChildrenView().get(name));
    }

    /**
     * 检查是否存在指定名称的子Actor
     */
    public boolean hasChild(String name) {
        return context.getChildrenView().containsKey(name);
    }

    /**
     * 创建子Actor
     * @param props Actor属性
     * @param name Actor名称
     * @return 子Actor引用
     */
    public <T> ActorRef<T> actorOf(Props<T> props, String name) {
        return context.actorOf(props, name);
    }

    /**
     * 获取指定名称的子Actor
     * @param name Actor名称
     * @return 子Actor引用
     */
    public ActorRef<Object> getChild(String name) {
        return (ActorRef<Object>) context.getChildrenView().get(name);
    }
} 