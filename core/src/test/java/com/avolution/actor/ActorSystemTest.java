package com.avolution.actor;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActorSystemTest {

    ActorSystem system;

    @BeforeEach
    void setUp() {
        system = new ActorSystem("mySystem");
    }

    @Test
    void test() {

        ActorRef<String> helloActor = system.actorOf(Props.create(HelloActor.class), "helloActor");

        helloActor.tell("world",null);

        // 等待一段时间后终止系统
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testEcho() {


    }

    @Test
    void testCreateChildActor() {


    }

    @AfterEach
    void tearDown() {
        system.terminate();
    }
}
