package com.avolution.actor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActorSystemTest {

    ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("mySystem");
    }

    @Test
    void test() {
        // 创建Actor实例
        ActorRef helloActor = system.actorOf(Props.create(HelloActor.class), "hello");

        // 发送消息
        helloActor.tellMessage("Hello!", null);

        // 等待一段时间后终止系统
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        system.terminate();
    }

}
