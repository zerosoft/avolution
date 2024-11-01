package com.avolution.actor.message;

/**
 * 系统消息 - 用于停止Actor的特殊消息
 * 当Actor收到此消息时，将触发其停止流程
 */
public final class PoisonPill {
    
    /**
     * 单例实例
     */
    public static final PoisonPill INSTANCE = new PoisonPill();

    /**
     * 私有构造函数防止外部创建实例
     */
    private PoisonPill() {}

    @Override
    public String toString() {
        return "PoisonPill";
    }
} 