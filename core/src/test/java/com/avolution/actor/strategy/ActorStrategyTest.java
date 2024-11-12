package com.avolution.actor.strategy;

import com.avolution.actor.core.*;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.supervision.Directive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ActorStrategyTest {
    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @Test
    @DisplayName("测试默认策略的消息处理")
    void testDefaultStrategyMessageHandling() throws Exception {
        TestActor actor = new TestActor();
        ActorRef<Message> actorRef = system.actorOf(Props.create(() -> actor), "test-actor");

        actorRef.tell(new Message("test"), ActorRef.noSender());
        Thread.sleep(100);

        assertEquals(1, actor.getMessageCount());
        assertEquals("test", actor.getLastMessage());
    }

    @Test
    @DisplayName("测试自定义策略的消息拦截")
    void testCustomStrategyInterception() throws Exception {
        AtomicBoolean beforeCalled = new AtomicBoolean(false);
        AtomicBoolean afterCalled = new AtomicBoolean(false);

        TestActor actor = new TestActor();
        actor.setStrategy(new DefaultActorStrategy<Message>() {
            @Override
            public void beforeMessageHandle(Envelope<Message> message, AbstractActor<Message> self) {
                beforeCalled.set(true);
            }

            @Override
            public void afterMessageHandle(Envelope<Message> message, AbstractActor<Message> self, boolean success) {
                afterCalled.set(true);
            }
        });

        ActorRef<Message> actorRef = system.actorOf(Props.create(() -> actor), "interceptor-actor");
        actorRef.tell(new Message("test"), ActorRef.noSender());
        Thread.sleep(100);

        assertTrue(beforeCalled.get());
        assertTrue(afterCalled.get());
    }

    @Test
    @DisplayName("测试错误处理策略")
    void testErrorHandlingStrategy() throws Exception {
        AtomicInteger restartCount = new AtomicInteger(0);

        ErrorActor actor = new ErrorActor();
        actor.setStrategy(new DefaultActorStrategy<Message>() {
            @Override
            public void handleFailure(Throwable cause, Envelope<Message> message, AbstractActor<Message> self) {
                restartCount.incrementAndGet();
                self.getContext().restart(cause);
            }
        });

        ActorRef<Message> actorRef = system.actorOf(Props.create(() -> actor), "error-actor");
        actorRef.tell(new Message("trigger-error"), ActorRef.noSender());
        Thread.sleep(100);

        assertEquals(1, restartCount.get());
        assertTrue(actor.wasPreRestartCalled());
        assertTrue(actor.wasPostRestartCalled());
    }

    // 测试用的消息类
    public static class Message {
        private final String content;

        Message(String content) {
            this.content = content;
        }

        String getContent() {
            return content;
        }
    }

    // 测试Actor
    public static class TestActor extends AbstractActor<Message> {
        private int messageCount = 0;
        private String lastMessage = null;

        @Override
        public void onReceive(Message message) {
            messageCount++;
            lastMessage = message.getContent();
        }

        int getMessageCount() {
            return messageCount;
        }

        String getLastMessage() {
            return lastMessage;
        }
    }

    // 错误测试Actor
    public static class ErrorActor extends AbstractActor<Message> {
        private boolean preRestartCalled = false;
        private boolean postRestartCalled = false;

        @Override
        public void onReceive(Message message) {
            if ("trigger-error".equals(message.getContent())) {
                throw new RuntimeException("Test error");
            }
        }

        @Override
        public void onPreRestart(Throwable reason) {
            preRestartCalled = true;
        }

        @Override
        public void onPostRestart(Throwable reason) {
            postRestartCalled = true;
        }

        boolean wasPreRestartCalled() {
            return preRestartCalled;
        }

        boolean wasPostRestartCalled() {
            return postRestartCalled;
        }
    }
}