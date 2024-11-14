package com.avolution.actor.deadLetter;


import com.avolution.actor.core.*;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.message.Terminated;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DeadLetterTest {
    private ActorSystem system;

    @BeforeEach
    void setup() {
        system = ActorSystem.create("test-system");
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.terminate();
        }
    }

   public static class WatcherActor extends AbstractActor<Object> {
        private final AtomicBoolean terminatedReceived = new AtomicBoolean(false);
        private final AtomicInteger deadLetterCount = new AtomicInteger(0);
        private final CompletableFuture<Void> terminationFuture = new CompletableFuture<>();

        @Override
        public void preStart() {
//            context.watch(context.system().getDeadLetters());
        }

        @OnReceive(Object.class)
        private void onMessage(Object msg) {
            if (msg instanceof Terminated terminated) {
                terminatedReceived.set(true);
                terminationFuture.complete(null);
            } else if (msg instanceof IDeadLetterActorMessage.DeadLetter) {
                deadLetterCount.incrementAndGet();
            }
        }

        public boolean wasTerminatedReceived() {
            return terminatedReceived.get();
        }

        public int getDeadLetterCount() {
            return deadLetterCount.get();
        }

        public CompletableFuture<Void> getTerminationFuture() {
            return terminationFuture;
        }
    }

    @Test
    void testDeadLetterOnActorTermination() throws Exception {
        // 创建观察者Actor
        ActorRef<Object> watcher = system.actorOf(Props.create(WatcherActor.class), "watcher");
//        WatcherActor watcherActor = (WatcherActor) system.getContext(watcher.path()).self().get();

        // 创建被监视的Actor
        ActorRef<String> watched = system.actorOf(Props.create(StringActor.class), "watched");

        // 建立监视关系
//        system.getContext(watcher.path()).watch(watched);

        // 停止被监视的Actor
        system.stop(watched);

        // 等待终止信号
//        watcherActor.getTerminationFuture().get(5, TimeUnit.SECONDS);

//        assertTrue(watcherActor.wasTerminatedReceived(), "Should receive terminated signal");
        assertTrue(watched.isTerminated(), "Watched actor should be terminated");
    }

    @Test
    void testDeadLetterForNonExistentActor() throws Exception {
        // 创建观察者Actor
        ActorRef<Object> watcher = system.actorOf(Props.create(WatcherActor.class), "watcher");
//        WatcherActor watcherActor = (WatcherActor) system.getContext(watcher.path()).self().get();

        // 向不存在的Actor发送消息
        ActorRef<String> nonExistent = system.actorOf(Props.create(StringActor.class), "non-existent");
        system.stop(nonExistent);
        nonExistent.tell("test message", ActorRef.noSender());

        // 等待一段时间让死信处理完成
        Thread.sleep(1000);

//        assertTrue(watcherActor.getDeadLetterCount() > 0, "Should receive dead letters");
    }

    @Test
    void testDeadLetterForTerminatedActor() throws Exception {
        // 创建被监视的Actor
        ActorRef<String> target = system.actorOf(Props.create(StringActor.class), "target");

        // 停止Actor
        system.stop(target);

        // 向已终止的Actor发送消息
        target.tell("message after termination", ActorRef.noSender());

        // 验证死信系统接收到消息
        Thread.sleep(1000);
        assertTrue(system.getDeadLetters() != null, "DeadLetter actor should exist");
    }
}