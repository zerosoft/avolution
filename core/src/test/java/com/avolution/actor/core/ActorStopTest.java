package com.avolution.actor.core;

import com.avolution.actor.pattern.ASK;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ActorStopTest {

    private static final Logger logger = LoggerFactory.getLogger(ActorStopTest.class);

    @Test
    public void testSingleActorStop() {
        ActorSystem system = ActorSystem.create("test-system");

        // 创建一个简单的测试Actor
        Props<String> props = Props.create(TestActor.class);
        ActorRef<String> actor = system.actorOf(props, "test-actor");

        // 创建一个简单的测试Actor
        props = Props.create(TestActor.class);
        ActorRef<String> actor_wait = system.actorOf(props, "test-actor-wait");

        // 发送一些消息
        actor.tell("message1", ActorRef.noSender());
        actor.tell("message2", ActorRef.noSender());
        try {
            Thread.sleep(100); // 增加等待时间
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


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
        logger.info("Starting parent-child actor stop test");

        // 创建父Actor
        Props<Object> parentProps = Props.create(ParentActor.class);
        ActorRef<Object> parent = system.actorOf(parentProps, "parent");


        try {
            ActorRef<Object> ask = ASK.ask(parent, new CreateChild("child1"), Duration.ofSeconds(1));
            ActorRef<Object> ask1 = ASK.ask(parent, new CreateChild("child2"), Duration.ofSeconds(1));

            ask.tell(new CreateChild("child11"), parent);
            ask.tell(new CreateChild("child12"), parent);

            ask1.tell(new CreateChild("child121"), parent);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        // 等待子Actor创建完成
        try {
            Thread.sleep(200); // 增加等待时间

            // 验证子Actor创建
//            Optional<ActorContext> parentContext = system.getContextManager().getContext(parent.path());
//            assertTrue(parentContext.isPresent());
//            assertEquals(2, parentContext.get().getChildren().size());

            // 停止父Actor
            CompletableFuture<Void> stopFuture = system.stop(parent);
            stopFuture.get(100, TimeUnit.SECONDS); // 使用get等待完成

            // 验证清理
//            assertFalse(system.hasActor(parent.path()));
            assertTrue(parent.isTerminated());



        } catch (Exception e) {
            logger.error("Test failed", e);
            fail("Test failed with exception: " + e.getMessage());
        }
        try {
            TimeUnit.SECONDS.sleep(20); // 增加等待时间
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    @Test
    public void testActorSystemStop() {
        ActorSystem system = ActorSystem.create("test-system");

        // 创建一个简单的测试Actor
        Props<String> props = Props.create(TestActor.class);
        ActorRef<String> actor = system.actorOf(props, "test-actor");

        // 创建一个简单的测试Actor
        props = Props.create(TestActor.class);
        ActorRef<String> actor_wait = system.actorOf(props, "test-actor-wait");

        // 验证Actor已停止
        actor.tell("message3", ActorRef.noSender());

        system.terminate();

        try {
            Thread.sleep(200); // 增加等待时间
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 测试用的Actor类
    private static class TestActor extends TypedActor<String> {
        @Override
        public void onReceive(String message) {
            logger.info("Received message: {}", message);
            try {
                Thread.sleep(100); // 模拟处理时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class ParentActor extends TypedActor<Object> {
        private final List<ActorRef<String>> children = new ArrayList<>();

        @Override
        public void onReceive(Object message) {
            if (message instanceof CreateChild) {
                CreateChild createChild = (CreateChild) message;
                Props<Object> childProps = Props.create(ParentActor.class);
                ActorRef<Object> stringActorRef = getContext().actorOf(childProps, createChild.name);
                getSender().tell(stringActorRef,getSelf());
            }
        }


    }

    private record CreateChild(String name) {}

}
