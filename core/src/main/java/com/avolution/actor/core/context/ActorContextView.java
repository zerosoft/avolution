package com.avolution.actor.core.context;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;

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
    public Optional<ActorRef<?>> getParent() {
        ActorContext parent = context.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        // 确保返回父Actor的引用
        return Optional.of(parent.getUnTypedActor().getSelfRef());
    }

    /**
     * 获取子Actor引用集合
     */
    public Set<ActorRef<?>> getChildren() {
        return new HashSet<>(context.getChildrenView().values());
    }

    /**
     * 查找指定名称的子Actor
     */
    public Optional<ActorRef<?>> findChild(String name) {
        return Optional.ofNullable(context.getChildrenView().get(name));
    }

    /**
     * 检查是否存在指定名称的子Actor
     */
    public boolean hasChild(String name) {
        return context.getChildrenView().containsKey(name);
    }
} 