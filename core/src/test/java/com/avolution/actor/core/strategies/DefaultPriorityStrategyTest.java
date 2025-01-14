package com.avolution.actor.core.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Priority;

@DisplayName("默认优先级策略测试")
class DefaultPriorityStrategyTest {
    
    private static class DefaultPriorityStrategy implements PriorityStrategy {
        @Override
        public Priority getPriority(Envelope envelope) {
            return envelope.getPriority();
        }

        @Override
        public boolean isHighPriority(Envelope envelope) {
            return envelope.getPriority() == Priority.HIGH;
        }

        @Override
        public int comparePriority(Envelope e1, Envelope e2) {
            return e2.getPriority().compareTo(e1.getPriority());
        }
    }

    private PriorityStrategy priorityStrategy;
    private Envelope highPriorityEnvelope;
    private Envelope normalPriorityEnvelope;
    private Envelope lowPriorityEnvelope;

    @BeforeEach
    void setUp() {
        priorityStrategy = new DefaultPriorityStrategy();
        
        highPriorityEnvelope = Envelope.builder()
                .message("high priority")
                .priority(Priority.HIGH)
                .build();
                
        normalPriorityEnvelope = Envelope.builder()
                .message("normal priority")
                .priority(Priority.NORMAL)
                .build();
                
        lowPriorityEnvelope = Envelope.builder()
                .message("low priority")
                .priority(Priority.LOW)
                .build();
    }

    @Test
    @DisplayName("测试优先级判断")
    void shouldIdentifyPriorityCorrectly() {
        assertTrue(priorityStrategy.isHighPriority(highPriorityEnvelope));
        assertFalse(priorityStrategy.isHighPriority(normalPriorityEnvelope));
        assertFalse(priorityStrategy.isHighPriority(lowPriorityEnvelope));
    }

    @Test
    @DisplayName("测试优先级比较")
    void shouldComparePrioritiesCorrectly() {
        // 高优先级应该大于普通优先级
        assertTrue(priorityStrategy.comparePriority(highPriorityEnvelope, normalPriorityEnvelope) > 0);
        
        // 普通优先级应该大于低优先级
        assertTrue(priorityStrategy.comparePriority(normalPriorityEnvelope, lowPriorityEnvelope) > 0);
        
        // 相同优先级应该相等
        assertEquals(0, priorityStrategy.comparePriority(normalPriorityEnvelope, normalPriorityEnvelope));
    }

    @Test
    @DisplayName("测试获取优先级")
    void shouldGetPriorityCorrectly() {
        assertEquals(Priority.HIGH, priorityStrategy.getPriority(highPriorityEnvelope));
        assertEquals(Priority.NORMAL, priorityStrategy.getPriority(normalPriorityEnvelope));
        assertEquals(Priority.LOW, priorityStrategy.getPriority(lowPriorityEnvelope));
    }
} 