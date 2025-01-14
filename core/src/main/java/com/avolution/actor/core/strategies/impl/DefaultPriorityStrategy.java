package com.avolution.actor.core.strategies.impl;

import com.avolution.actor.core.strategies.PriorityStrategy;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.message.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认优先级策略实现
 * 处理消息的优先级排序和判断
 */
public class DefaultPriorityStrategy implements PriorityStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPriorityStrategy.class);

    /**
     * 获取消息优先级
     * 如果消息没有指定优先级，则根据消息类型分配默认优先级
     */
    @Override
    public Priority getPriority(Envelope envelope) {
        if (envelope == null) {
            logger.warn("Envelope is null, returning lowest priority");
            return Priority.LOW;
        }

        // 如果已设置优先级，直接返回
        if (envelope.getPriority() != null) {
            return envelope.getPriority();
        }

        // 根据消息类型分配默认优先级
        return switch (envelope.getMessageType()) {
            case SYSTEM -> Priority.HIGH;
            case SIGNAL -> Priority.NORMAL;
            case NORMAL -> Priority.LOW;
            default -> Priority.LOW;
        };
    }

    /**
     * 判断是否为高优先级消息
     * 系统消息和信号消息被视为高优先级
     */
    @Override
    public boolean isHighPriority(Envelope envelope) {
        if (envelope == null) {
            return false;
        }

        // 优先级判断
        if (envelope.getPriority() == Priority.HIGH) {
            return true;
        }

        // 消息类型判断
        return envelope.getMessageType() == MessageType.SYSTEM ||
               envelope.getMessageType() == MessageType.SIGNAL;
    }

    /**
     * 比较两个消息的优先级
     * 返回值：
     * - 负数：e1优先级高于e2
     * - 0：优先级相等
     * - 正数：e2优先级高于e1
     */
    @Override
    public int comparePriority(Envelope e1, Envelope e2) {
        if (e1 == e2) return 0;
        if (e1 == null) return 1;
        if (e2 == null) return -1;

        // 首先比较优先级
        int priorityCompare = getPriority(e1).getValue() - getPriority(e2).getValue();
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        // 优先级相同时，比较时间戳
        return Long.compare(e1.getCreatedAt().getEpochSecond(), e2.getCreatedAt().getEpochSecond());
    }

    /**
     * 获取消息的优先级值
     * 用于优先级队列的排序
     */
    public int getPriorityValue(Envelope envelope) {
        Priority priority = getPriority(envelope);
        return priority != null ? priority.getValue() : Priority.LOW.getValue();
    }
}