package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.core.context.ActorContextManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@DisplayName("ActorContextManager 测试")
class ActorContextManagerTest {
    private ActorSystem system;
    private ActorContextManager contextManager;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
        contextManager = new ActorContextManager();
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.terminate();
        }
    }

    @Test
    @DisplayName("测试路径规范化和上下文添加")
    void testPathNormalizationAndContextAddition() {
        TestActor actor = new TestActor();
        ActorContext context = new ActorContext("/user/test", system, actor, null, Props.create(TestActor.class));

        contextManager.addContext("/user/test", context);
        assertTrue(contextManager.hasContext("/user/test"));

        contextManager.addContext("/user/test2", context);
        assertTrue(contextManager.hasContext("/user/test2"));
    }

    @Test
    @DisplayName("测试层级结构管理")
    void testHierarchyManagement() {
        // 创建父上下文
        ActorContext parentContext = new ActorContext("/user/parent", system, new TestActor(), null, Props.create(TestActor.class));
        contextManager.addContext("/user/parent", parentContext);

        // 创建子上下文
        ActorContext child1 = new ActorContext("/user/parent/child1", system, new TestActor(), parentContext, Props.create(TestActor.class));
        ActorContext child2 = new ActorContext("/user/parent/child2", system, new TestActor(), parentContext, Props.create(TestActor.class));

        contextManager.addContext("/user/parent/child1", child1);
        contextManager.addContext("/user/parent/child2", child2);

        // 验证层级关系
        Set<ActorContext> children = contextManager.getChildContexts("/user/parent");
        assertEquals(2, children.size());
        assertTrue(children.contains(child1));
        assertTrue(children.contains(child2));
    }

    @Test
    @DisplayName("测试上下文移除")
    void testContextRemoval() {
        ActorContext context = new ActorContext("/user/test", system, new TestActor(), null, Props.create(TestActor.class));
        contextManager.addContext("/user/test", context);

        assertTrue(contextManager.hasContext("/user/test"));
        contextManager.removeContext("/user/test");
        assertFalse(contextManager.hasContext("/user/test"));
    }

    @Test
    @DisplayName("测试级联终止")
    void testCascadingTermination() {
        // 创建层级结构
        ActorContext parent = new ActorContext("/user/parent", system, new TestActor(), null, Props.create(TestActor.class));
        ActorContext child1 = new ActorContext("/user/parent/child1", system, new TestActor(), parent, Props.create(TestActor.class));
        ActorContext child2 = new ActorContext("/user/parent/child2", system, new TestActor(), parent, Props.create(TestActor.class));

        contextManager.addContext("/user/parent", parent);
        contextManager.addContext("/user/parent/child1", child1);
        contextManager.addContext("/user/parent/child2", child2);

        // 终止父上下文
        contextManager.removeContext("/user/parent");

        // 验证所有相关上下文都被移除
        assertFalse(contextManager.hasContext("/user/parent"));
        assertFalse(contextManager.hasContext("/user/parent/child1"));
        assertFalse(contextManager.hasContext("/user/parent/child2"));
    }

    @Test
    @DisplayName("测试并发操作")
    void testConcurrentOperations() throws Exception {
        int threadCount = 10;
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                TestActor actor = new TestActor();
                ActorContext context = new ActorContext("/user/test" + index, system, actor, null, Props.create(TestActor.class));
                contextManager.addContext("/user/test" + index, context);
            });
        }

        CompletableFuture.allOf(futures).get();
        assertEquals(threadCount, contextManager.getContextCount());
    }

    // 测试用Actor类
    private static class TestActor extends AbstractActor<Object> {
        @Override
        public void onReceive(Object message) {
            // 测试实现
        }
    }
}