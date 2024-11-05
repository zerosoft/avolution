package com.avolution.actor;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
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
        helloActor.tell(Thread.currentThread().threadId() +" world",null);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executorService.submit(() -> {
                helloActor.tell(Thread.currentThread().threadId() +" world ="+ finalI +"",null);
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
