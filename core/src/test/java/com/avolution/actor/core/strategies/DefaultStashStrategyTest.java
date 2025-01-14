package com.avolution.actor.core.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.avolution.actor.core.strategies.impl.DefaultStashStrategy;
import com.avolution.actor.message.Envelope;

@DisplayName("默认暂存策略测试")
class DefaultStashStrategyTest {

    private DefaultStashStrategy stashStrategy;
    private Envelope envelope;

    @BeforeEach
    void setUp() {
        stashStrategy = new DefaultStashStrategy();
        envelope = Envelope.builder()
                .message("test message")
                .build();
    }

    @Test
    @DisplayName("测试默认暂存行为")
    void shouldFollowDefaultStashBehavior() {
        // 默认不应该暂存消息
        assertFalse(stashStrategy.shouldStash(envelope));
        
        // 默认应该允许取出暂存消息
        assertTrue(stashStrategy.shouldUnstash(envelope));
    }

    @Test
    @DisplayName("测试最大暂存容量")
    void shouldHaveCorrectMaxStashSize() {
        assertEquals(100, stashStrategy.getMaxStashSize());
    }
} 