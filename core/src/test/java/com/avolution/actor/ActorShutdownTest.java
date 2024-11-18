package com.avolution.actor;

import com.avolution.actor.core.*;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.message.Signal;
import com.avolution.actor.pattern.ASK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ActorShutdownTest {

    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("ShutdownTestSystem");
    }

   public static class ShutdownTestActor extends AbstractActor<ShutdownTestActor.Message> {
        private Logger logger = org.slf4j.LoggerFactory.getLogger(ShutdownTestActor.class);

        public interface Message {}

        public static class LongProcessingMessage implements Message {
            private final Duration processingTime;

            public LongProcessingMessage(Duration processingTime) {
                this.processingTime = processingTime;
            }
        }

       public static class CreateChildMessage implements Message {
           private final Duration processingTime;

           public CreateChildMessage(Duration processingTime) {
               this.processingTime = processingTime;
           }
       }

        private final AtomicBoolean postStopCalled = new AtomicBoolean(false);
        private final AtomicInteger messageCount = new AtomicInteger(0);

        @OnReceive(LongProcessingMessage.class)
        private void onLongProcessing(LongProcessingMessage msg) throws InterruptedException {
            messageCount.incrementAndGet();
            System.out.println(path()+"Processing message");
        }

        @OnReceive(CreateChildMessage.class)
        private void onCreateChild(CreateChildMessage msg) {
            ActorRef<ShutdownTestActor.Message> child = context.actorOf(Props.create(ShutdownTestActor.class),"Child");
            getSender().tell(child, getSelf());
        }

        @Override
        public void onPostStop() {
            System.out.println(path()+"Actor stopped");
            postStopCalled.set(true);
        }

        public boolean wasPostStopCalled() {
            return postStopCalled.get();
        }

        public int getProcessedMessageCount() {
            return messageCount.get();
        }
    }

    @Test
    void testGracefulShutdown() throws Exception {
        // 1. 创建测试Actor
        ActorRef<ShutdownTestActor.Message> actor =
                system.actorOf(Props.create(ShutdownTestActor.class), "shutdown-test");

        ActorRef<ShutdownTestActor.Message> asked = ASK.ask(actor, new ShutdownTestActor.CreateChildMessage(Duration.ofMillis(500)), Duration.ofSeconds(1));

        // 2. 发送长时间处理的消息
        asked.tell(new ShutdownTestActor.LongProcessingMessage(Duration.ofMillis(500)), ActorRef.noSender());

        actor.tell(Signal.KILL, ActorRef.noSender());

        // 4. 验证关闭过程
        Thread.sleep(3000); // 等待关闭开始

    }

    @Test
    void testForceShutdown() throws Exception {
        ActorRef<ShutdownTestActor.Message> actor =
                system.actorOf(Props.create(ShutdownTestActor.class), "force-shutdown-test");
//        ShutdownTestActor testActor = (ShutdownTestActor) system.getContext(actor.path()).self().get();
//
//        // 发送多个消息
//        for (int i = 0; i < 5; i++) {
//            actor.tell(new ShutdownTestActor.LongProcessingMessage(Duration.ofMillis(100)),
//                    ActorRef.noSender());
//        }
//
//        // 立即停止系统
//        system.stop(actor);
//
//        Thread.sleep(200); // 等待停止完成
//
//        assertTrue(testActor.isTerminated(), "Actor should be terminated");
//        assertTrue(testActor.wasPostStopCalled(), "postStop should have been called");
    }
}