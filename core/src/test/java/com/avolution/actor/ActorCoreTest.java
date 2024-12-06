package com.avolution.actor;

import com.avolution.actor.core.*;
import com.avolution.actor.message.Signal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@DisplayName("Actor 核心功能测试")
class ActorCoreTest {
    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.terminate();
        }
    }

    @Test
    @DisplayName("测试Actor创建和路径生成")
    void testActorCreationAndPath() {
        // 创建父Actor
        ActorRef<TestMessage> parent = system.actorOf(Props.create(TestActor.class), "parent");
        assertNotNull(parent);
        System.out.println(parent.path());
        assertTrue(parent.path().matches("/user/parent#[\\w-]+"));

        // 创建子Actor
        TestActor parentActor = new TestActor();
        ActorRef<TestMessage> parentRef = system.actorOf(Props.create(() -> parentActor), "parent2");
        ActorRef<TestMessage> child = parentActor.getContext().actorOf(Props.create(TestActor.class), "child");
        ActorRef<TestMessage> child2 = parentActor.getContext().actorOf(Props.create(TestActor.class), "child");

        ActorRef<TestMessage> child3 = parentActor.getContext().actorOf(Props.create(TestActor.class), "child");

        System.out.println(child.name());
        assertNotNull(child);
        assertTrue(child.path().startsWith(parentRef.path()));

        Set<String> children = system.getRefRegistry().getChildren(parentRef.path());

        assertTrue(children.contains(child.path()));


        child3.tell(Signal.KILL,ActorRef.noSender());

        try {
            TimeUnit.MILLISECONDS.sleep(100L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        children = system.getRefRegistry().getChildren(parentRef.path());
        assertFalse(children.contains(child3.path()));


        parentRef.tell(Signal.KILL,ActorRef.noSender());

        try {
            TimeUnit.MILLISECONDS.sleep(100L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        children = system.getRefRegistry().getChildren(parentRef.path());
        assertEquals(children.size(),0);

        child3.tell(new TestMessage("test"),ActorRef.noSender());
    }

    @Test
    @DisplayName("测试Actor生命周期回调")
    void testActorLifecycle() throws Exception {
        TestLifecycleActor actor = new TestLifecycleActor();
        ActorRef<TestMessage> ref = system.actorOf(Props.create(() -> actor), "lifecycle");

        // 验证初始化回调
        assertTrue(actor.wasPreStartCalled());

        // 发送消息
        ref.tell(new TestMessage("test"), ActorRef.noSender());
        Thread.sleep(100);
        assertEquals(1, actor.getProcessedCount());

        // 停止Actor
        ref.tell(Signal.KILL, ActorRef.noSender());
        Thread.sleep(100);
        assertTrue(actor.wasPostStopCalled());
        assertTrue(ref.isTerminated());
    }

    @Test
    @DisplayName("测试Actor层级关系和停止传播")
    void testActorHierarchyAndTermination() throws Exception {
        // 创建父子Actor结构
        ParentActor parent = new ParentActor();
        ActorRef<TestMessage> parentRef = system.actorOf(Props.create(() -> parent), "parent");

        // 创建子Actor
        parentRef.tell(new TestMessage("create-child"), ActorRef.noSender());
        Thread.sleep(100);

        // 验证子Actor创建
        assertEquals(2, parent.getChildCount());

        // 停止父Actor
        system.stop(parentRef);
        Thread.sleep(200);

        // 验证所有Actor都已停止
        assertTrue(parentRef.isTerminated());
        assertTrue(parent.areChildrenTerminated());
    }

    // 测试消息类
    private static class TestMessage {
        private final String content;
        TestMessage(String content) { this.content = content; }
        String getContent() { return content; }
    }

    // 基础测试Actor
    private static class TestActor extends TypedActor<TestMessage> {
        private final AtomicInteger processedCount = new AtomicInteger(0);

        @Override
        public void onReceive(TestMessage message) {
            processedCount.incrementAndGet();
        }

        int getProcessedCount() {
            return processedCount.get();
        }
    }

    // 生命周期测试Actor
    private static class TestLifecycleActor extends TestActor {
        private final AtomicBoolean preStartCalled = new AtomicBoolean(false);
        private final AtomicBoolean postStopCalled = new AtomicBoolean(false);

        boolean wasPreStartCalled() { return preStartCalled.get(); }
        boolean wasPostStopCalled() { return postStopCalled.get(); }
    }

    // 父Actor测试类
    private static class ParentActor extends TypedActor<TestMessage> {
        private final AtomicInteger childCount = new AtomicInteger(0);
        private final AtomicBoolean childrenTerminated = new AtomicBoolean(false);

        @Override
        public void onReceive(TestMessage message) {
            if ("create-child".equals(message.getContent())) {
                context.actorOf(Props.create(TestActor.class), "child" + childCount.incrementAndGet());
                context.actorOf(Props.create(TestActor.class), "child" + childCount.incrementAndGet());
            }
        }


        int getChildCount() { return childCount.get(); }
        boolean areChildrenTerminated() { return childrenTerminated.get(); }
    }
}