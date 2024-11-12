package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.message.PoisonPill;
import com.avolution.actor.pattern.ASK;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ActorContextTest {
    private ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @Test
    @DisplayName("测试Actor创建和路径管理")
    void testActorCreationAndPath() {
        TestActor parent = new TestActor();
        ActorRef<Message> parentRef = system.actorOf(Props.create(() -> parent), "parent");

        // 创建子Actor
        ActorRef<Message> child = parent.getContext().actorOf(Props.create(TestActor.class), "child");

        assertEquals(parentRef.path() + "/child", child.path());
        assertTrue(parent.getContext().getChildren().containsValue(child));
    }

    @Test
    @DisplayName("测试Actor停止和资源清理")
    void testActorStopAndCleanup() {
        TestActor parent = new TestActor();
        ActorRef<Message> parentRef = system.actorOf(Props.create(() -> parent), "parent");
        ActorRef<Message> child = parent.getContext().actorOf(Props.create(TestActor.class), "child");

        parent.getContext().stop(child);

        assertFalse(parent.getContext().getChildren().containsValue(child));
    }

    @Test
    @DisplayName("测试监督策略和错误处理")
    void testSupervisionAndErrorHandling() {
        TestSupervisor supervisor;
        ActorRef<Message> supervisorRef = system.actorOf(Props.create(TestSupervisor.class), "supervisor");

        try {
             supervisor= ASK.ask(supervisorRef,new Message("hello"),Duration.ofSeconds(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建错误Actor
        ActorRef<Message> errorActor = supervisor.getContext().actorOf(Props.create(ErrorActor.class), "error");

        // 触发错误
        errorActor.tell(new Message("trigger-error"), ActorRef.noSender());

        assertTrue(supervisor.wasErrorHandled());
        assertEquals(Directive.RESTART, supervisor.getLastDirective());
    }

    @Test
    @DisplayName("测试Actor重启机制")
    void testActorRestart() {
        ErrorActor actor = new ErrorActor();
        ActorRef<Message> actorRef = system.actorOf(Props.create(() -> actor), "error");

        actor.getContext().restart(new RuntimeException("test"));

        assertTrue(actor.wasPreRestartCalled());
        assertTrue(actor.wasPostRestartCalled());
    }

    // 测试消息类
    private static class Message {
        private final String content;

        Message(String content) {
            this.content = content;
        }

        String getContent() {
            return content;
        }
    }

    // 测试Actor
    private static class TestActor extends AbstractActor<Message> {
        @Override
        public void onReceive(Message message) {
            // 简单实现
        }
    }

    // 测试监督Actor
    private static class TestSupervisor extends AbstractActor<Message> {
        private final AtomicBoolean errorHandled = new AtomicBoolean(false);
        private volatile Directive lastDirective;

        @Override
        public void preStart() {
            // 简单实现
            getContext().setSupervisorStrategy(new SupervisorStrategy() {
                @Override
                public Directive handle(Throwable cause) {
                    errorHandled.set(true);
                    lastDirective = Directive.RESTART;
                    return lastDirective;
                }

                @Override
                public int getMaxRetries() {
                    return 3;
                }

                @Override
                public Duration getWithinTimeRange() {
                    return Duration.ofMinutes(1);
                }
            });
        }

        @Override
        public void onReceive(Message message) {
            // 简单实现
            getSender().tell(this,getSelf());
        }

        boolean wasErrorHandled() {
            return errorHandled.get();
        }

        Directive getLastDirective() {
            return lastDirective;
        }
    }

    // 错误测试Actor
    private static class ErrorActor extends AbstractActor<Message> {
        private final AtomicBoolean preRestartCalled = new AtomicBoolean(false);
        private final AtomicBoolean postRestartCalled = new AtomicBoolean(false);

        @Override
        public void onReceive(Message message) {
            if ("trigger-error".equals(message.getContent())) {
                throw new RuntimeException("Test error");
            }
        }

        @Override
        public void onPreRestart(Throwable reason) {
            preRestartCalled.set(true);
        }

        @Override
        public void onPostRestart(Throwable reason) {
            postRestartCalled.set(true);
        }

        boolean wasPreRestartCalled() {
            return preRestartCalled.get();
        }

        boolean wasPostRestartCalled() {
            return postRestartCalled.get();
        }
    }
}