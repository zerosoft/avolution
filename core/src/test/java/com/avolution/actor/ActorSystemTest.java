package com.avolution.actor;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.message.Signal;
import com.avolution.actor.pattern.ASK;

public class ActorSystemTest {

    ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("mySystem");
    }

    @Test
    void testActorCreation() {
        // 测试创建 Actor
        ActorRef<HelloActorMessage> helloActor = system.actorOf(Props.create(HelloActor.class), "helloActor");
        assertNotNull(helloActor, "Actor should be created successfully");
        assertEquals("helloActor", helloActor.name(), "Actor name should match");
    }

    @Test
    void testActorMessageHandling() {
        ActorRef<HelloActorMessage> helloActor = system.actorOf(Props.create(HelloActor.class), "helloActor");
        helloActor.tell(new HelloActorMessage.Hello(), null);

//        ExecutorService executorService = Executors.newCachedThreadPool();
//        for (int i = 0; i < 10; i++) {
//            int finalI = i;
//            executorService.submit(() -> {
//                helloActor.tell(new HelloActorMessage.World(), null);
//            });
//        }

        try {
            Object ask = ASK.ask(helloActor, new HelloActorMessage.World(), Duration.ofSeconds(50));
            System.out.println(ask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 等待一段时间后终止系统
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testUniqueIdGeneration() {
        ActorRef<HelloActorMessage> actor1 = system.actorOf(Props.create(HelloActor.class), "actor1");
        ActorRef<HelloActorMessage> actor2 = system.actorOf(Props.create(HelloActor.class), "actor2");

        assertNotEquals(actor1.path(), actor2.path(), "Actor paths should be unique");
    }

    @Test
    void testCreateChildActor() {
        ActorRef<HelloActorMessage> parentActor = system.actorOf(Props.create(HelloActor.class), "parentActor");
        ActorRef<HelloActorMessage> childActor = system.actorOf(Props.create(HelloActor.class), "childActor");

        assertNotNull(childActor, "Child actor should be created successfully");
        assertTrue(childActor.path().contains("childActor"), "Child actor path should contain its name");
    }

    @Test
    void testOptimizedPath() {
        ActorRef<HelloActorMessage> actor = system.actorOf(Props.create(HelloActor.class), "testActor_0A");
        String path = actor.path();
        assertTrue(path.matches("/user/testActor_[0-9a-fA-F-]+"), "Path should contain the new unique ID");
    }

    @Test
    void testActorTermination() {
        ActorRef<HelloActorMessage> actor = system.actorOf(Props.create(HelloActor.class), "testActor");
        assertFalse(actor.isTerminated(), "Actor should not be terminated");
        actor.tell(new HelloActorMessage.Terminate(), null);
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        actor.tell(Signal.KILL, ActorRef.noSender());
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(actor.isTerminated(), "Actor should be terminated");
    }

    @Test
    void testCreateMultipleActors() {
        for (int i = 0; i < 5; i++) {
            ActorRef<HelloActorMessage> actor = system.actorOf(Props.create(HelloActor.class), "actor" + i);
            assertNotNull(actor, "Actor " + i + " should be created successfully");
            assertEquals("actor" + i, actor.name(), "Actor name should match");
        }
    }

    @AfterEach
    void tearDown() {
        system.terminate();
    }
}
