package com.avolution.actor;


import com.avolution.actor.core.*;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.pattern.ASK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AbstractActorTest {
    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @Test
    @DisplayName("测试基本消息处理")
    void testBasicMessageHandling() throws Exception {
        ActorRef<TestMessage> actor = system.actorOf(Props.create(TestActor.class), "test-actor");

        // 发送消息并等待处理
        CompletableFuture<Object> future = ASK.ask(actor, new TestMessage("hello"), Duration.ofSeconds(5));
        TestActor testActor = (TestActor)future.get();

        assertEquals(1, testActor.getProcessedCount());
    }

    @Test
    @DisplayName("测试生命周期回调")
    void testLifecycleCallbacks() throws Exception {
        TestActor testActor = new TestActor();
        ActorRef<TestMessage> actor = system.actorOf(Props.create(() -> testActor), "lifecycle-test");

        // 验证初始化回调
        assertTrue(testActor.wasPreStartCalled());

        // 发送终止消息
        actor.tell(PoisonPill.INSTANCE, ActorRef.noSender());
        Thread.sleep(100);

        assertTrue(testActor.wasPostStopCalled());
    }

    @Test
    @DisplayName("测试错误处理和重启")
    void testErrorHandlingAndRestart() throws Exception {
        ActorRef<TestMessage> actor = system.actorOf(Props.create(TestActor.class), "error-test");

        // 发送触发错误的消息
        actor.tell(new TestMessage("error"), ActorRef.noSender());
        Thread.sleep(100);

        // 发送消息并等待处理
        CompletableFuture<Object> future = ASK.ask(actor, new TestMessage("hello"), Duration.ofSeconds(5));
        TestActor testActor = (TestActor)future.get();

        assertTrue(testActor.wasPreRestartCalled());
        assertTrue(testActor.wasPostRestartCalled());
    }

    @Test
    @DisplayName("测试子Actor创建和管理")
    void testChildActorManagement() throws Exception {
        ActorRef<TestMessage> parentActor = system.actorOf(Props.create(TestActor.class), "parent");

        // 发送创建子Actor的消息
        parentActor.tell(new TestMessage("create-child"), ActorRef.noSender());

        // 发送消息并等待处理
        CompletableFuture<Object> future = ASK.ask(parentActor, new TestMessage("hello"), Duration.ofSeconds(5));

        TestActor testActor = (TestActor)future.get();
        assertNotNull(testActor.getChild());
        assertTrue(testActor.getChild().path().startsWith(parentActor.path()));
    }

    // 测试消息类
    private static class TestMessage {
        private final String content;

        TestMessage(String content) {
            this.content = content;
        }

        String getContent() {
            return content;
        }
    }

    // 测试Actor类
    public static class TestActor extends AbstractActor<TestMessage> {
        private final AtomicInteger processedCount = new AtomicInteger(0);
        private final AtomicBoolean preStartCalled = new AtomicBoolean(false);
        private final AtomicBoolean postStopCalled = new AtomicBoolean(false);
        private final AtomicBoolean preRestartCalled = new AtomicBoolean(false);
        private final AtomicBoolean postRestartCalled = new AtomicBoolean(false);
        private ActorRef<TestMessage> child;

        @OnReceive(TestMessage.class)
        public void processTestMessage(TestMessage message) {
            processedCount.incrementAndGet();

            switch (message.getContent()) {
                case "error" -> throw new RuntimeException("Test error");
                case "create-child" -> child = getContext().actorOf(Props.create(TestActor.class), "child");
                default -> getSender().tell(this, getSelf());
            }
        }

        @Override
        public void preStart() {
            preStartCalled.set(true);
        }

        @Override
        public void onPostStop() {
            postStopCalled.set(true);
        }

        @Override
        public void onPreRestart(Throwable reason) {
            preRestartCalled.set(true);
            super.onPreRestart(reason);
        }

        @Override
        public void onPostRestart(Throwable reason) {
            postRestartCalled.set(true);
            super.onPostRestart(reason);
        }

        int getProcessedCount() {
            return processedCount.get();
        }

        boolean wasPreStartCalled() {
            return preStartCalled.get();
        }

        boolean wasPostStopCalled() {
            return postStopCalled.get();
        }

        boolean wasPreRestartCalled() {
            return preRestartCalled.get();
        }

        boolean wasPostRestartCalled() {
            return postRestartCalled.get();
        }

        ActorRef<TestMessage> getChild() {
            return child;
        }
    }
}