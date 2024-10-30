package com.avolution.actor;

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
        system = ActorSystem.create("mySystem");
    }

    @Test
    void test() {

        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executorService.submit(()->{
                // 创建Actor实例
                ActorRef helloActor = system.actorOf(Props.create(HelloActor.class), "hello"+ finalI);
                for (int j = 0; j < 10; j++) {
                    // 发送消息
                    helloActor.tellMessage(helloActor.path()+"===Hello!"+j, helloActor);
                }
            });
        }

        // 等待一段时间后终止系统
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testEcho() {

        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        // 创建Actor实例
        ActorRef helloActor = system.actorOf(Props.create(HelloActor.class), "hello"+ 1);
        // 创建Actor实例
        ActorRef helloActor2 = system.actorOf(Props.create(HelloActor.class), "hello"+ 2);

        helloActor.tellMessage("Hello Back",helloActor2);

        // 等待一段时间后终止系统
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCreateChildActor() {

        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        // 创建Actor实例
        ActorRef helloActor = system.actorOf(Props.create(HelloActor.class), "hello"+ 1);

        // 创建Actor实例
        ActorRef helloActor2 = system.actorOf(Props.create(HelloActor.class), "hello"+ 2);

        helloActor.tellMessage("Hello Back",helloActor2);

        // 等待一段时间后终止系统
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        system.terminate();
    }
}
