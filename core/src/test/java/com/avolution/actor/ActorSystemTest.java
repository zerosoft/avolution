package com.avolution.actor;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class ActorSystemTest {

    ActorSystem system;

    @BeforeEach
    void setUp() {
        system = new ActorSystem("mySystem");
    }

    @Test
    void test() {

        ActorRef<String> helloActor = system.actorOf(Props.create(HelloActor.class), "helloActor");
        helloActor.tell(Thread.currentThread().threadId() + " world", null);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executorService.submit(() -> {
                helloActor.tell(Thread.currentThread().threadId() + " world =" + finalI + "", null);
            });
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
        ActorRef<String> actor1 = system.actorOf(Props.create(HelloActor.class), "actor1");
        ActorRef<String> actor2 = system.actorOf(Props.create(HelloActor.class), "actor2");

        assertNotEquals(actor1.path(), actor2.path(), "Actor paths should be unique");
    }

    @Test
    void testCreateChildActor() {
        ActorRef<String> parentActor = system.actorOf(Props.create(HelloActor.class), "parentActor");
        ActorRef<String> childActor = parentActor.getContext().actorOf(Props.create(HelloActor.class), "childActor");

        assertNotNull(childActor, "Child actor should be created successfully");
        assertTrue(childActor.path().contains("childActor"), "Child actor path should contain its name");
    }

    @Test
    void testOptimizedPath() {
        ActorRef<String> actor = system.actorOf(Props.create(HelloActor.class), "testActor");
        String path = actor.path();
        assertTrue(path.matches("/user/testActor/[0-9a-fA-F-]+"), "Path should contain the new unique ID");
    }

    @AfterEach
    void tearDown() {
        system.terminate();
    }
}
