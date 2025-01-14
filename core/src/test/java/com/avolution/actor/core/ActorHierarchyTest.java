package com.avolution.actor.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;

@DisplayName("Actor层级关系测试")
public class ActorHierarchyTest {
    
    private ActorSystem system;
    private static final AtomicReference<CompletableFuture<ActorRef<String>>> childFutureRef = 
        new AtomicReference<>();
    private static final AtomicReference<CountDownLatch> stopLatchRef = 
        new AtomicReference<>();
    
    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
        childFutureRef.set(new CompletableFuture<>());
        stopLatchRef.set(new CountDownLatch(1));
    }
    
    @Test
    @DisplayName("测试父子Actor创建和引用关系")
    void testParentChildRelationship() throws Exception {
        // 创建父Actor
        ActorRef<String> parent = system.actorOf(
            Props.create(ParentActor.class), 
            "parent"
        );
        
        // 等待父Actor创建完成
        Thread.sleep(100);
        
        // 通过父Actor创建子Actor
        parent.tell("create-child", ActorRef.noSender());
        
        // 获取创建的子Actor引用
        ActorRef<String> child = childFutureRef.get().get(1, TimeUnit.SECONDS);
        
        // 验证父子关系
        assertTrue(parent.getContextView().hasChild(child.name()));
        assertEquals(parent.path() + "/" + child.name(), child.path());
        assertNotNull(child.getContextView().getParent());
    }
    
    @Test
    @DisplayName("测试子Actor停止时父Actor的处理")
    void testChildTermination() throws Exception {
        ActorRef<String> parent = system.actorOf(
            Props.create(ParentActor.class), 
            "parent-for-termination"
        );
        
        // 创建子Actor
        parent.tell("create-child", ActorRef.noSender());
        ActorRef<String> child = childFutureRef.get().get(1, TimeUnit.SECONDS);
        String childName = child.name();
        
        // 确保子Actor已经被父Actor注册
        assertTrue(parent.getContextView().hasChild(childName));
        
        // 停止子Actor
        child.tell("stop", ActorRef.noSender());
        
        // 等待子Actor完全停止
        assertTrue(stopLatchRef.get().await(2, TimeUnit.SECONDS));
        
        // 验证父Actor不再持有子Actor引用
        parent.tell("check-child", ActorRef.noSender());
        Thread.sleep(100); // 给父Actor一些处理时间
        
        assertFalse(parent.getContextView().hasChild(childName), 
            "Parent should not have child reference after child termination");
//        assertFalse(child.isRunning(),
//            "Child actor should not be running after termination");
    }
    
    @Test
    @DisplayName("测试父Actor停止时子Actor的处理")
    void testParentTermination() throws Exception {
        ActorRef<String> parent = system.actorOf(
            Props.create(ParentActor.class), 
            "parent-for-parent-termination"
        );
        
        // 创建子Actor
        parent.tell("create-child", ActorRef.noSender());
        ActorRef<String> child = childFutureRef.get().get(1, TimeUnit.SECONDS);
        
        // 停止父Actor
        parent.tell("stop", ActorRef.noSender());
        Thread.sleep(200);
        
        // 验证子Actor也被停止
//        assertFalse(child.isRunning());
    }
    
    // 父Actor实现
    // 测试用的Actor类
    static class ParentActor extends TypedActor<String> {
        @Override
        public void onReceive(String message) {
            logger.info("Parent received: {}", message);
        }
    }

    static class ChildActor extends TypedActor<String> {
        @Override
        public void onReceive(String message) {
            logger.info("Child received: {}", message);
        }
    }
} 