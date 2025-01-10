package com.avolution.actor.core.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.TypedActor;

public class ActorContextViewTest {

    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @Test
    void testContextViewHierarchy() {
        // 创建父Actor
        ActorRef<?> parent = system.actorOf(Props.create(ParentActor.class), "parent");
        
        // 通过父Actor的上下文创建子Actor
        ActorRef<?> child = parent.getContextView().actorOf(Props.create(ChildActor.class), "child");
        
        // 验证层级关系
        assertTrue(parent.getContextView().hasChild("child"));
        assertEquals(parent, child.getContextView().getParent());
        assertTrue(parent.getContextView().getChildren().contains(child));
        assertEquals(child, parent.getContextView().getChild("child"));
    }

    @Test
    void testMultipleChildren() {
        ActorRef<?> parent = system.actorOf(Props.create(ParentActor.class), "parent");
        
        ActorRef<?> child1 = parent.getContextView().actorOf(Props.create(ChildActor.class), "child1");
        ActorRef<?> child2 = parent.getContextView().actorOf(Props.create(ChildActor.class), "child2");
        
        assertTrue(parent.getContextView().hasChild("child1"));
        assertTrue(parent.getContextView().hasChild("child2"));
        assertEquals(2, parent.getContextView().getChildren().size());
        assertTrue(parent.getContextView().getChildren().contains(child1));
        assertTrue(parent.getContextView().getChildren().contains(child2));
    }

    @Test
    void testDeepHierarchy() {
        ActorRef<?> grandparent = system.actorOf(Props.create(ParentActor.class), "grandparent");
        ActorRef<?> parent = grandparent.getContextView().actorOf(Props.create(ParentActor.class), "parent");
        ActorRef<?> child = parent.getContextView().actorOf(Props.create(ChildActor.class), "child");
        
        assertEquals(grandparent, parent.getContextView().getParent());
        assertEquals(parent, child.getContextView().getParent());
        assertTrue(grandparent.getContextView().hasChild("parent"));
        assertTrue(parent.getContextView().hasChild("child"));
    }

    static class ParentActor extends TypedActor<Object> {
        @Override
        protected void onReceive(Object message) throws Exception {
            // 父Actor的消息处理
        }
    }

    static class ChildActor extends TypedActor<Object> {
        @Override
        protected void onReceive(Object message) throws Exception {
            // 子Actor的消息处理
        }
    }
} 