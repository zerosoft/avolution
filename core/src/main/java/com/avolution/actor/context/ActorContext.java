package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.supervision.SupervisorStrategy;

import java.time.Duration;

/**
 * Actor上下文接口，提供Actor运行时环境
 * @param <T> Actor处理的消息类型
 */
public interface ActorContext<T> {
    /**
     * 获取当前Actor的引用
     */
    ActorRef<T> self();
    
    /**
     * 获取当前消息的发送者
     */
    ActorRef<?> sender();
    
    /**
     * 获取Actor系统
     */
    ActorSystem system();
    
    /**
     * 创建子Actor
     */
    ActorRef<T> spawn(Props props, String name);
    
    /**
     * 监视另一个Actor
     */
    void watch(ActorRef<?> other);
    
    /**
     * 停止监视另一个Actor
     */
    void unwatch(ActorRef<?> other);
    
    /**
     * 停止子Actor
     */
    void stop(ActorRef<?> child);
    
    /**
     * 设置接收超时
     */
    void setReceiveTimeout(Duration timeout);
    
    /**
     * 获取父Actor
     */
    ActorRef<?> parent();
    
    /**
     * 获取监督策略
     */
    SupervisorStrategy supervisorStrategy();
}

