package com.avolution.actor.core.strategies;

import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Priority;

/**
 * 消息优先级策略接口
 */
public interface PriorityStrategy {
    
    /**
     * 获取消息的优先级
     * @param envelope 消息信封
     * @return 消息优先级
     */
    Priority getPriority(Envelope envelope);
    
    /**
     * 判断是否为高优先级消息
     * @param envelope 消息信封
     * @return 是否为高优先级
     */
    boolean isHighPriority(Envelope envelope);
    
    /**
     * 比较两个消息的优先级
     * @param e1 第一个消息信封
     * @param e2 第二个消息信封
     * @return 比较结果
     */
    int comparePriority(Envelope e1, Envelope e2);
}
