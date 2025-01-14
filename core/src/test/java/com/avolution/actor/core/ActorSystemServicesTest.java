package com.avolution.actor.core;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.message.Signal;
import com.avolution.actor.pattern.ASK;
import com.avolution.actor.system.actor.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ActorSystemServicesTest {
    
    private ActorSystem system;
    
    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }
    
    @AfterEach
    void tearDown() {
        if (system != null) {
            system.terminate();
        }
    }

    @Test
    @Timeout(5)
    void testDeadLetterService() throws Exception {
        // 创建测试Actor
        TestActor testActor = new TestActor();
        ActorRef<Object> actorRef = system.actorOf(Props.create(TestActor.class), "test-actor");
        
        // 停止Actor
        system.stop(actorRef);
        Thread.sleep(100); // 等待Actor完全停止
        
        // 发送消息给已停止的Actor
        actorRef.tell("test message", ActorRef.noSender());
        
        // 验证消息被转发到死信Actor
        ActorRef<IDeadLetterActorMessage> deadLetterActor = system.getDeadLetters();
        Integer ask = ASK.ask(deadLetterActor, new IDeadLetterActorMessage.DeadLetterCount(), Duration.ofSeconds(3));
        assertTrue(ask!=0);
    }

    @Test
    void testSystemGuardianService() {
        // 获取系统守护者
        ActorRef<SystemGuardianActorMessage> guardian = system.getSystemGuardian();
        assertNotNull(guardian);
        
        // 验证系统守护者状态
//        assertTrue(guardian.getContext().isRunning());
        assertEquals("/system", guardian.path());
    }

    @Test
    void testUserGuardianService() throws Exception {
        // 创建用户Actor
        CompletableFuture<ActorRef<Object>> future = new CompletableFuture<>();
        Props<Object> props = Props.create(TestActor.class);
        
//        system.getSystemGuardian().tell(
//            new UserGuardianActorMessage.CreateUserActor("test-actor", props, future),
//            ActorRef.noSender()
//        );
        
        ActorRef<Object> actorRef = future.get(3, TimeUnit.SECONDS);
        assertNotNull(actorRef);
        assertTrue(actorRef.path().startsWith("/user"));
    }

    @Test
    void testSchedulerService() throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        TestActor testActor = new TestActor();
        ActorRef<Object> actorRef = system.actorOf(Props.create(TestActor.class), "scheduled-actor");
        
        // 调度一次性消息
        system.getScheduler().schedule(
            () -> actorRef.tell("scheduled", ActorRef.noSender()),
            100,
            TimeUnit.MILLISECONDS
        );
        
        Thread.sleep(200);
        assertTrue(testActor.hasReceivedMessage("scheduled"));
    }

    // 测试用Actor类
    private static class TestActor extends TypedActor<Object> {
        private String lastMessage;
        
        @Override
        protected void onReceive(Object message) {
            lastMessage = message.toString();
        }
        
        public boolean hasReceivedMessage(String message) {
            return message.equals(lastMessage);
        }
    }
} 