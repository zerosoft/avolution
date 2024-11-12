package com.avolution.actor.context;


import com.avolution.actor.core.*;
import com.avolution.actor.message.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ActorContextManagerTest {
    private ActorSystem system;
    private ActorContextManager contextManager;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
        contextManager = new ActorContextManager();
    }

    @Test
    @DisplayName("测试添加和获取上下文")
    void testAddAndGetContext() {
        // 创建测试Actor和上下文
        TestActor actor = new TestActor();
        ActorContext context = new ActorContext("/user/test", system, actor, null, Props.create(TestActor.class));

        // 添加上下文
        contextManager.addContext("/user/test", context);

        // 验证上下文是否正确添加
        assertTrue(contextManager.hasContext("/user/test"));
        assertEquals(1, contextManager.getContextCount());

        // 验证可以正确获取上下文
        Optional<ActorContext> retrievedContext = contextManager.getContext("/user/test");
        assertTrue(retrievedContext.isPresent());
        assertEquals(context, retrievedContext.get());
    }

    @Test
    @DisplayName("测试获取子上下文")
    void testGetChildContexts() {
        // 创建父Actor上下文
        ActorContext parentContext = new ActorContext("/user/parent", system, new TestActor(), null, Props.create(TestActor.class));
        contextManager.addContext("/user/parent", parentContext);

        // 创建子Actor上下文
        ActorContext child1Context = new ActorContext("/user/parent/child1", system, new TestActor(), parentContext, Props.create(TestActor.class));
        ActorContext child2Context = new ActorContext("/user/parent/child2", system, new TestActor(), parentContext, Props.create(TestActor.class));

        contextManager.addContext("/user/parent/child1", child1Context);
        contextManager.addContext("/user/parent/child2", child2Context);

        // 获取子上下文
        Set<ActorContext> children = contextManager.getChildContexts("/user/parent");

        assertEquals(2, children.size());
        assertTrue(children.contains(child1Context));
        assertTrue(children.contains(child2Context));
    }

    @Test
    @DisplayName("测试终止所有上下文")
    void testTerminateAll() {
        // 创建测试Actor和上下文
        TestActor actor1 = new TestActor();
        TestActor actor2 = new TestActor();

        ActorContext context1 = new ActorContext("/user/test1", system, actor1, null, Props.create(TestActor.class));
        ActorContext context2 = new ActorContext("/user/test2", system, actor2, null, Props.create(TestActor.class));

        contextManager.addContext("/user/test1", context1);
        contextManager.addContext("/user/test2", context2);

        // 终止所有上下文
        contextManager.terminateAll();

        // 验证所有上下文已被移除
        assertEquals(0, contextManager.getContextCount());
        assertFalse(contextManager.hasContext("/user/test1"));
        assertFalse(contextManager.hasContext("/user/test2"));

        // 验证Actor的postStop被调用
        assertTrue(actor1.wasPostStopCalled());
        assertTrue(actor2.wasPostStopCalled());
    }

    // 测试用的Actor类
    private static class TestActor extends AbstractActor<Object> {
        private final AtomicBoolean postStopCalled = new AtomicBoolean(false);

        @Override
        public void onReceive(Object message) {
            // 测试用，不需要实现
        }

        @Override
        public void onPostStop() {
            postStopCalled.set(true);
        }

        public boolean wasPostStopCalled() {
            return postStopCalled.get();
        }
    }
}