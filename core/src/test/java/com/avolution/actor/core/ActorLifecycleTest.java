package com.avolution.actor.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.avolution.actor.core.lifecycle.LifecycleState;

class ActorLifecycleTest {

    @Test
    @Timeout(5)
    void testActorLifecycle() throws Exception {
        ActorSystem system = ActorSystem.create("lifecycle-test");
        
        // 创建生命周期测试Actor
        LifecycleTestActor testActor = new LifecycleTestActor();
        ActorRef<Object> actorRef = system.actorOf(Props.create(LifecycleTestActor.class), "lifecycle-actor");
        
        // 验证启动状态
        assertEquals(LifecycleState.RUNNING, testActor.getLifecycleState());
        assertTrue(testActor.isPreStartCalled());
        
        // 测试停止
        CompletableFuture<Void> stopFuture = system.stop(actorRef);
        stopFuture.get(3, TimeUnit.SECONDS);
        
        assertTrue(testActor.isPreStopCalled());
        assertEquals(LifecycleState.STOPPED, testActor.getLifecycleState());
        
        system.terminate();
    }

    private static class LifecycleTestActor extends TypedActor<Object> {
        private boolean preStartCalled = false;
        private boolean preStopCalled = false;
        
        @Override
        public boolean preStart() {
            preStartCalled = true;
            return true;
        }
        
        @Override
        public boolean preStop() {
            preStopCalled = true;
            return true;
        }
        
        @Override
        protected void onReceive(Object message) {
            // 测试消息处理
        }
        
        public boolean isPreStartCalled() {
            return preStartCalled;
        }
        
        public boolean isPreStopCalled() {
            return preStopCalled;
        }
        
        public LifecycleState getLifecycleState() {
            return getContext().getLifecycle().getState();
        }
    }
} 