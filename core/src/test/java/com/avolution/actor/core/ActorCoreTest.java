package com.avolution.actor.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.message.Signal;

@ExtendWith(MockitoExtension.class)
public class ActorCoreTest {

    private ActorSystem actorSystem;
    
    @Mock
    private ActorRef mockActorRef;
    
    @Mock
    private ActorContext mockContext;

    @BeforeEach
    void setUp() {
        actorSystem = ActorSystem.create("test-system");
    }

    @Test
    void testActorCreation() {
        ActorRef<TestMessage> actor = actorSystem.actorOf(
            Props.create(TestActor.class),
            "test-actor"
        );
        
        assertNotNull(actor);
        assertEquals("test-actor", actor.name());
        assertFalse(actor.isTerminated());
    }

    @Test
    void testMessageSending() {
        ActorRef<TestMessage> actor = actorSystem.actorOf(
            Props.create(TestActor.class),
            "test-actor"
        );
        
        TestMessage msg = new TestMessage("test");
        actor.tell(msg, ActorRef.noSender());
        
        // 等待消息处理
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void testActorLifecycle() {
        ActorRef<TestMessage> actor = actorSystem.actorOf(
            Props.create(TestActor.class),
            "lifecycle-test"
        );
        
        CompletableFuture<Void> stopFuture = actorSystem.stop(actor);
        
        try {
            stopFuture.get(5, TimeUnit.SECONDS);
            assertTrue(actor.isTerminated());
        } catch (Exception e) {
            fail("Actor停止失败: " + e.getMessage());
        }
    }

    @Test
    void testSignalHandling() {
        ActorRef<TestMessage> actor = actorSystem.actorOf(
            Props.create(TestActor.class),
            "signal-test"
        );
        
        actor.tell(Signal.SUSPEND, ActorRef.noSender());
        actor.tell(Signal.RESUME, ActorRef.noSender());
        
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 测试消息类
    static class TestMessage {
        private final String content;
        
        TestMessage(String content) {
            this.content = content;
        }
        
        String getContent() {
            return content;
        }
    }
    
    // 测试Actor类
    static class TestActor extends TypedActor<TestMessage> {
        
        @Override
        public void onReceive(TestMessage message) {
            // 简单打印消息内容
            System.out.println("Received: " + message.getContent());
        }
    }
} 