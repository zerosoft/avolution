package com.avolution.actor.core.strategies;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.avolution.actor.core.strategies.impl.DefaultRetryStrategy;
import com.avolution.actor.message.Envelope;

@DisplayName("默认重试策略测试")
class DefaultRetryStrategyTest {

    private DefaultRetryStrategy retryStrategy;
    private Envelope envelope;

    @BeforeEach
    void setUp() {
        retryStrategy = new DefaultRetryStrategy();
        envelope = Envelope.builder()
                .message("test message")
                .build();
    }

    @Test
    @DisplayName("测试重试次数限制")
    void shouldRespectMaxRetries() {
        // 未达到最大重试次数时应该继续重试
        assertTrue(retryStrategy.shouldRetry(envelope));
        
        // 设置重试次数为最大值
        envelope.incrementRetryCount();
        envelope.incrementRetryCount();
        envelope.incrementRetryCount();
        
        // 达到最大重试次数后不应该继续重试
        assertFalse(retryStrategy.shouldRetry(envelope));
    }

    @Test
    @DisplayName("测试重试延迟时间")
    void shouldCalculateExponentialBackoff() {
        // 第一次重试延迟
        assertEquals(Duration.ofMillis(100), retryStrategy.getRetryDelay(0));
        
        // 第二次重试延迟(200ms)
        assertEquals(Duration.ofMillis(200), retryStrategy.getRetryDelay(1));
        
        // 第三次重试延迟(400ms)
        assertEquals(Duration.ofMillis(400), retryStrategy.getRetryDelay(2));
    }
} 