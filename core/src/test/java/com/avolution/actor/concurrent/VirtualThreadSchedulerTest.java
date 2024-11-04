package com.avolution.actor.concurrent;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadSchedulerTest {
    
    private VirtualThreadScheduler scheduler;
    
    @BeforeEach
    void setUp() {
        scheduler = new VirtualThreadScheduler();
    }
    
    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }
    
    @Test
    void testScheduleRunnable() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(0);
        
        scheduler.schedule(() -> {
            result.set(42);
            latch.countDown();
        }, 100, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(42, result.get());
    }
    
    @Test
    void testScheduleCallable() throws Exception {
        ScheduledFuture<Integer> future = scheduler.schedule(
            () -> 42,
            100,
            TimeUnit.MILLISECONDS
        );
        
        assertEquals(42, future.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testScheduleAtFixedRate() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger(0);
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> {
                counter.incrementAndGet();
                latch.countDown();
            },
            100,
            100,
            TimeUnit.MILLISECONDS
        );
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        future.cancel(true);
        assertEquals(3, counter.get());
    }
    
    @Test
    void testParallelExecution() throws Exception {
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);
        List<Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            futures.add(scheduler.submit(() -> {
                Thread.sleep(100);
                latch.countDown();
                return taskId;
            }));
        }
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(taskCount, futures.size());
        
        for (int i = 0; i < taskCount; i++) {
            assertEquals(i, futures.get(i).get());
        }
    }
    
    @Test
    void testShutdown() throws Exception {
        assertFalse(scheduler.isShutdown());
        scheduler.shutdown();
        assertTrue(scheduler.isShutdown());
        
        assertThrows(RejectedExecutionException.class, () ->
            scheduler.schedule(() -> {}, 1, TimeUnit.SECONDS)
        );
    }
    
    @Test
    void testGracefulShutdown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.schedule(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);
        
        scheduler.shutdown();
        assertTrue(scheduler.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(0, latch.getCount());
    }
} 