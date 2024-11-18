package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.core.context.ActorRefRegistry;
import com.avolution.actor.message.Signal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@DisplayName("ActorRefRegistry 测试")
class ActorRefRegistryTest {
    private ActorSystem system;
    private ActorRefRegistry registry;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
        registry = new ActorRefRegistry(system);
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.terminate();
        }
    }

    @Test
    @DisplayName("测试Actor注册和查询")
    void testActorRegistration() {
        TestActor actor = new TestActor();
        ActorRef<TestMessage> ref = system.actorOf(Props.create(() -> actor), "test");
        registry.register(ref, null);

//        assertTrue(registry.contains(ref.path()));
        assertEquals(ref, registry.getRef(ref.path()).orElse(null));
    }

    @Test
    @DisplayName("测试父子关系管理")
    void testParentChildRelationship() {
        TestActor parentActor = new TestActor();
        ActorRef<TestMessage> parentRef = system.actorOf(Props.create(() -> parentActor), "parent");
        registry.register(parentRef, null);

        TestActor childActor = new TestActor();
        ActorRef<TestMessage> childRef = system.actorOf(Props.create(() -> childActor), "child");
        registry.register(childRef, parentRef.path());

        Set<String> children = registry.getChildren(parentRef.path());
        assertTrue(children.contains(childRef.path()));
        assertEquals(1, children.size());
    }

    @Test
    @DisplayName("测试Actor注销")
    void testActorUnregistration() {
        TestActor actor = new TestActor();
        ActorRef<TestMessage> ref = system.actorOf(Props.create(() -> actor), "test");
        registry.register(ref, null);

        registry.unregister(ref.path(), "Test unregister");

//        assertFalse(registry.contains(ref.path()));
        assertTrue(registry.getRef(ref.path()).isEmpty());
    }

    @Test
    @DisplayName("测试Actor监视关系")
    void testWatchRelationship() {
        TestActor watchedActor = new TestActor();
        ActorRef<TestMessage> watchedRef = system.actorOf(Props.create(() -> watchedActor), "watched");
        registry.register(watchedRef, null);

        TestActor watcherActor = new TestActor();
        ActorRef<TestMessage> watcherRef = system.actorOf(Props.create(() -> watcherActor), "watcher");
        registry.register(watcherRef, null);

        registry.watch(watcherRef, watchedRef);
        registry.unregister(watchedRef.path(), "Test termination");

        assertTrue(watcherActor.hasReceivedTerminated());
    }

    @Test
    @DisplayName("测试取消监视")
    void testUnwatchRelationship() {
        TestActor watchedActor = new TestActor();
        ActorRef<TestMessage> watchedRef = system.actorOf(Props.create(() -> watchedActor), "watched");
        registry.register(watchedRef, null);

        TestActor watcherActor = new TestActor();
        ActorRef<TestMessage> watcherRef = system.actorOf(Props.create(() -> watcherActor), "watcher");
        registry.register(watcherRef, null);

        registry.watch(watcherRef, watchedRef);
//        registry.unwatch(watcherRef, watchedRef);
        registry.unregister(watchedRef.path(), "Test termination");

        assertFalse(watcherActor.hasReceivedTerminated());
    }

    @Test
    @DisplayName("测试多重监视关系")
    void testMultipleWatchRelationships() {
        TestActor watchedActor = new TestActor();
        ActorRef<TestMessage> watchedRef = system.actorOf(Props.create(() -> watchedActor), "watched");
        registry.register(watchedRef, null);

        TestActor watcher1 = new TestActor();
        TestActor watcher2 = new TestActor();
        ActorRef<TestMessage> watcher1Ref = system.actorOf(Props.create(() -> watcher1), "watcher1");
        ActorRef<TestMessage> watcher2Ref = system.actorOf(Props.create(() -> watcher2), "watcher2");

        registry.register(watcher1Ref, null);
        registry.register(watcher2Ref, null);

        registry.watch(watcher1Ref, watchedRef);
        registry.watch(watcher2Ref, watchedRef);

        registry.unregister(watchedRef.path(), "Test termination");
        watchedRef.tell(Signal.KILL,ActorRef.noSender());

        try {
            TimeUnit.MILLISECONDS.sleep(50L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(watcher1.hasReceivedTerminated());
        assertTrue(watcher2.hasReceivedTerminated());
    }

    private static class TestMessage {
        private final String content;
        TestMessage(String content) { this.content = content; }
    }

    private static class TestActor extends AbstractActor<TestMessage> {
        private final AtomicBoolean receivedTerminated = new AtomicBoolean(false);

        @Override
        public void onReceive(TestMessage message) {
            // 测试实现
        }

        @Override
        public void onPostStop() {
            receivedTerminated.set(true);
        }

        boolean hasReceivedTerminated() {
            return receivedTerminated.get();
        }
    }
}