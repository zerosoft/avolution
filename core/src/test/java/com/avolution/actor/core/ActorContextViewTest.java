package com.avolution.actor.core;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.avolution.actor.core.context.ActorContextView;

public class ActorContextViewTest extends BaseActorTest {

    @Test
    void testContextViewBasicOperations() throws InterruptedException {
        // 创建父Actor
        ActorRef<Object> parentActor = system.actorOf(Props.create(TestActor.class), "parent");
        // 等待Actor创建完成
        TimeUnit.MILLISECONDS.sleep(100);
        
        ActorContextView parentView = parentActor.getContextView();

        // 验证基本属性
        assertEquals("/user/parent", parentView.getPath());
        assertNotNull(parentView.getSystem());
        assertTrue(parentView.getParent().isPresent());
        assertTrue(parentView.getChildren().isEmpty());
    }

    @Test
    void testContextViewHierarchy() throws InterruptedException {
        // 创建父Actor
        ActorRef<Object> parentActor = system.actorOf(Props.create(TestActor.class), "parent");
        // 等待父Actor创建完成
        TimeUnit.MILLISECONDS.sleep(100);
        
        ActorContextView parentView = parentActor.getContextView();

        // 通过父Actor的上下文创建子Actor
        ActorRef<Object> childActor = parentActor.getContext().actorOf(Props.create(TestActor.class), "child");
        // 等待子Actor创建完成
        TimeUnit.MILLISECONDS.sleep(100);
        
        // 验证父子关系
        assertTrue(parentView.hasChild("child"), "Parent should have child");
        assertEquals(1, parentView.getChildren().size(), "Parent should have exactly one child");
        assertTrue(parentView.findChild("child").isPresent(), "Should find child by name");
        assertEquals(childActor, parentView.findChild("child").get(), "Child reference should match");

        // 验证子Actor的父引用
        ActorContextView childView = childActor.getContextView();
        assertTrue(childView.getParent().isPresent(), "Child should have parent");
        assertEquals(parentActor, childView.getParent().get(), "Parent reference should match");
    }

    static class TestActor extends TypedActor<Object> {
        @Override
        protected void onReceive(Object message) throws Exception {
            // 测试Actor，不需要实际处理消息
        }
    }
} 