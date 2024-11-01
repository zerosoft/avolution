package com.avolution.actor.router;

/**
 * 池大小调整策略
 */
public enum ResizeStrategy {
    NONE,           // 不调整
    LOAD_BASED,     // 基于负载调整
    METRIC_BASED,   // 基于度量调整
    ADAPTIVE        // 自适应调整
}
