package com.avolution.actor.context;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.IActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.supervision.SupervisorStrategy;

public interface ActorContext {
    /**
     * 获取父Actor上下文
     */
    ActorContext getParent();
    
    /**
     * 获取当前Actor的ActorRef
     */
    ActorRef self();
    
    /**
     * 创建子Actor
     */
    ActorRef actorOf(Props props, String name);
    
    /**
     * 停止指定的Actor
     */
    void stop(ActorRef actor);
    
    /**
     * 获取消息发送者
     */
    ActorRef sender();
    
    /**
     * 获取Actor系统
     */
    IActorSystem system();
    
    /**
     * 获取监督策略
     */
    SupervisorStrategy supervisorStrategy();
    
    /**
     * 获取子Actor
     */
    Iterable<ActorRef> getChildren();
    
    /**
     * 获取Actor路径
     */
    String path();
    
    /**
     * 监视另一个Actor
     */
    void watch(ActorRef actor);
    
    /**
     * 取消监视另一个Actor
     */
    void unwatch(ActorRef actor);
} 