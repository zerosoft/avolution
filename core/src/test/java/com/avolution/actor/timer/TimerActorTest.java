package com.avolution.actor.timer;


import com.avolution.actor.core.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class TimerActorTest {

    @Test
    void testActorTimer() throws Exception {
        ActorSystem system = new ActorSystem("TimerTestSystem");

        try {
            // 创建带定时器的Actor
            ActorRef<TimerHelloActor.Message> timerActor =
                    system.actorOf(Props.create(TimerHelloActor.class), "timer-hello");

            // 启动定时器，每500毫秒执行一次
            timerActor.tell(
                    new TimerHelloActor.StartTimer(Duration.ofMillis(500)),
                    ActorRef.noSender()
            );

            // 等待几个定时器周期
            Thread.sleep(2000);

            // 检查定时器执行次数
            CompletableFuture<Object> future = timerActor.ask(
                    new TimerHelloActor.GetTimerCount(),
                    Duration.ofSeconds(1)
            );

            Integer count = (Integer) future.get();
            System.out.println(count);
            assertTrue(count >= 3, "Timer should have ticked at least 3 times");

        } finally {
            // 关闭Actor系统
            system.terminate();
        }
    }
}