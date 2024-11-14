package com.avolution.actor.core;

import com.avolution.actor.core.context.ActorContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ActorStopTest {
    @Test
    public void testSingleActorStop() {
        ActorSystem system = ActorSystem.create("test-system");

        // 创建一个简单的测试Actor
        Props<String> props = Props.create(TestActor.class);
        ActorRef<String> actor = system.actorOf(props, "test-actor");

        // 发送一些消息
        actor.tell("message1", ActorRef.noSender());
        actor.tell("message2", ActorRef.noSender());

        // 停止Actor
        CompletableFuture<Void> stopFuture = system.stop(actor);
        stopFuture.join();

        // 验证Actor已停止
        assertTrue(actor.isTerminated());
        actor.tell("message3", ActorRef.noSender());

        system.terminate();
    }

    @Test
    public void testParentChildActorStop() {
        ActorSystem system = ActorSystem.create("test-system");

        // 创建父Actor
        Props<Object> parentProps = Props.create(ParentActor.class);
        ActorRef<Object> parent = system.actorOf(parentProps, "parent");

        // 通过父Actor创建子Actor
        parent.tell(new CreateChild("child1"), ActorRef.noSender());
        parent.tell(new CreateChild("child2"), ActorRef.noSender());

        // 等待子Actor创建完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 获取父Actor的Context
        Optional<ActorContext> parentContext = system.getContextManager().getContext(parent.path());
        assertTrue(parentContext.isPresent());

        // 获取子Actor列表
        Map<String, ActorRef> children = parentContext.get().getChildren();
        assertFalse(children.isEmpty());
        assertEquals(2, children.size());

        // 停止父Actor
        CompletableFuture<Void> stopFuture = system.stop(parent);
        stopFuture.join();

        // 验证父Actor已停止
        assertTrue(parent.isTerminated());

        // 验证所有子Actor都已停止
        children.values().forEach(child ->
                assertTrue(child.isTerminated())
        );

        system.terminate();
    }

    @Test
    public void testMultipleActorsStop() {
        ActorSystem system = ActorSystem.create("test-system");
        List<ActorRef<String>> actors = new ArrayList<>();

        // 创建多个Actor
        for (int i = 0; i < 10; i++) {
            Props<String> props = Props.create(TestActor.class);
            ActorRef<String> actor = system.actorOf(props, "test-actor-" + i);
            actors.add(actor);

            // 发送一些消息
            for (int j = 0; j < 5; j++) {
                actor.tell("message-" + j, ActorRef.noSender());
            }
        }

        // 并发停止所有Actor
        List<CompletableFuture<Void>> stopFutures = actors.stream()
                .map(system::stop)
                .collect(Collectors.toList());

        CompletableFuture.allOf(stopFutures.toArray(new CompletableFuture[0])).join();

        // 验证所有Actor都已停止
        assertTrue(actors.stream().allMatch(ActorRef::isTerminated));

        system.terminate();
    }

    // 测试用的Actor类
    private static class TestActor extends AbstractActor<String> {
        @Override
        public void onReceive(String message) {
            logger.info("Received message: {}", message);
            try {
                Thread.sleep(100); // 模拟处理时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        protected void onPostStop() {
            logger.info("Actor stopped"+name());
        }
    }

    private static class ParentActor extends AbstractActor<Object> {
        private final List<ActorRef<String>> children = new ArrayList<>();

        @Override
        public void onReceive(Object message) {
            if (message instanceof CreateChild) {
                CreateChild createChild = (CreateChild) message;
                Props<String> childProps = Props.create(TestActor.class);
                ActorRef<String> stringActorRef = getContext().actorOf(childProps, createChild.name);

            }
        }


    }

    private record CreateChild(String name) {}
}
