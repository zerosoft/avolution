package com.avolution.actor;

import com.avolution.actor.core.*;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.exception.AskTimeoutException;
import com.avolution.actor.pattern.ASK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ASKTest {
    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @Test
    @DisplayName("测试正常的Ask请求响应")
    void testNormalAskResponse() throws Exception {
        ActorRef<Message> actor = system.actorOf(Props.create(EchoActor.class), "echo");
        String response = ASK.ask(actor, new Message("hello"), Duration.ofSeconds(10));
        assertEquals("hello", response);
    }

    @Test
    @DisplayName("测试Ask超时")
    void testAskTimeout() {
        ActorRef<Message> actor = system.actorOf(Props.create(SlowActor.class), "slow");
        assertThrows(AskTimeoutException.class, () ->
                ASK.ask(actor, new Message("hello"), Duration.ofMillis(100))
        );
    }

    @Test
    @DisplayName("测试异步Ask")
    void testAsyncAsk() throws Exception {
        ActorRef<Message> actor = system.actorOf(Props.create(EchoActor.class), "async-echo");
        CompletableFuture<String> future = ASK.askAsync(actor, new Message("hello"), Duration.ofSeconds(1));

        String response = future.get(1, TimeUnit.SECONDS);
        assertEquals("hello", response);
    }

    @Test
    @DisplayName("测试Ask异常处理")
    void testAskWithException() {
        ActorRef<Message> actor = system.actorOf(Props.create(ErrorActor.class), "error");
        assertThrows(RuntimeException.class, () ->
                ASK.ask(actor, new Message("trigger-error"), Duration.ofSeconds(1))
        );
    }

    @Test
    @DisplayName("测试多个并发Ask")
    void testConcurrentAsks() throws Exception {
        ActorRef<Message> actor = system.actorOf(Props.create(EchoActor.class), "concurrent");

        CompletableFuture<String>[] futures = new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            futures[i] = ASK.askAsync(actor, new Message("msg-" + i), Duration.ofSeconds(1));
        }

        CompletableFuture.allOf(futures).get(2, TimeUnit.SECONDS);

        for (int i = 0; i < 5; i++) {
            assertEquals("msg-" + i, futures[i].get());
        }
    }

    // 测试用的消息类
    public static class Message {
        private final String content;

        Message(String content) {
            this.content = content;
        }

        String getContent() {
            return content;
        }
    }

    // 简单回显Actor
    public static class EchoActor extends AbstractActor<Message> {
        @OnReceive(Message.class)
        public void onMessage(Message msg) {
            getSender().tell(msg.getContent(), getSelf());
        }
    }

    // 慢响应Actor
    public static class SlowActor extends AbstractActor<Message> {
        @OnReceive(Message.class)
        public void onMessage(Message msg) {
            try {
                Thread.sleep(500);
                getSender().tell(msg.getContent(), getSelf());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 抛出异常的Actor
    public static class ErrorActor extends AbstractActor<Message> {
        @OnReceive(Message.class)
        public void onMessage(Message msg) {
            if ("trigger-error".equals(msg.getContent())) {
                throw new RuntimeException("Test error");
            }
            getSender().tell(msg.getContent(), getSelf());
        }
    }
}