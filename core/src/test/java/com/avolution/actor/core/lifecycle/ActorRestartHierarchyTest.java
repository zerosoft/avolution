package com.avolution.actor.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.TypedActor;

public class ActorRestartHierarchyTest {

    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @Test
    void testHierarchyPreservedOnRestart() throws Exception {
        // 创建父Actor和子Actors
        ActorRef<Object> parent = system.actorOf(Props.create(RestartableActor.class), "parent");
        ActorRef<Object> child1 = parent.getContextView().actorOf(Props.create(RestartableActor.class), "child1");
        ActorRef<Object> child2 = parent.getContextView().actorOf(Props.create(RestartableActor.class), "child2");

        // 记录初始状态
        String parentPath = parent.path();
        String child1Path = child1.path();
        String child2Path = child2.path();

        // 触发父Actor重启
        parent.tell(new ThrowException(), ActorRef.noSender());

        // 等待重启完成
        Thread.sleep(1000);

        // 验证层级关系保持
        assertEquals(parentPath, parent.path());
        assertEquals(2, parent.getContextView().getChildren().size());
        assertTrue(parent.getContextView().hasChild("child1"));
        assertTrue(parent.getContextView().hasChild("child2"));

        ActorRef<Object> newChild1 = parent.getContextView().getChild("child1");
        ActorRef<Object> newChild2 = parent.getContextView().getChild("child2");

        assertEquals(child1Path, newChild1.path());
        assertEquals(child2Path, newChild2.path());
    }

    @Test
    void testChildRestartDoesNotAffectParent() throws Exception {
        ActorRef<Object> parent = system.actorOf(Props.create(RestartableActor.class), "parent");
        ActorRef<Object> child = parent.getContextView().actorOf(Props.create(RestartableActor.class), "child");

        String parentPath = parent.path();
        String childPath = child.path();

        // 触发子Actor重启
        child.tell(new ThrowException(), ActorRef.noSender());

        // 等待重启完成
        Thread.sleep(1000);

        // 验证父Actor不受影响
        assertEquals(parentPath, parent.path());
        assertEquals(1, parent.getContextView().getChildren().size());
        assertTrue(parent.getContextView().hasChild("child"));

        ActorRef<Object> newChild = parent.getContextView().getChild("child");
        assertEquals(childPath, newChild.path());
    }

    @Test
    void testRestartWithCustomStrategy() throws Exception {
        ActorRef<Object> parent = system.actorOf(Props.create(SupervisorActor.class), "supervisor");
        ActorRef<Object> child = parent.getContextView().actorOf(Props.create(RestartableActor.class), "supervised");

        String childPath = child.path();

        // 触发子Actor重启
        child.tell(new ThrowException(), ActorRef.noSender());

        // 等待重启完成
        Thread.sleep(1000);

        // 验证子Actor被重启但保持在层级中
        assertTrue(parent.getContextView().hasChild("supervised"));
        ActorRef<Object> newChild = parent.getContextView().getChild("supervised");
        assertEquals(childPath, newChild.path());
    }

    static class ThrowException {
    }

    static class RestartableActor extends TypedActor<Object> {
        @Override
        protected void onReceive(Object message) throws Exception {
            if (message instanceof ThrowException) {
                throw new RuntimeException("Simulated failure");
            }
        }

        @Override
        public void preRestart(Throwable reason) {
            logger.info("Actor {} is about to restart", getPath());
            super.preRestart(reason);
        }

        @Override
        public void postRestart(Throwable reason) {
            logger.info("Actor {} has been restarted", getPath());
            super.postRestart(reason);
        }
    }

    static class SupervisorActor extends TypedActor<Object> {
        @Override
        protected void onReceive(Object message) throws Exception {
            if (message instanceof ThrowException) {
                // 当收到 ThrowException 消息时，重启当前 Actor
                getContext().restart(new RuntimeException("Simulated failure"));
            }
        }
    }
}