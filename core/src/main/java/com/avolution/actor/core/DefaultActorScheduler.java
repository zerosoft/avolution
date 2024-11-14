package com.avolution.actor.core;

import com.avolution.actor.concurrent.VirtualThreadScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DefaultActorScheduler implements ActorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultActorScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();;


    public DefaultActorScheduler() {
        this.scheduler = new VirtualThreadScheduler();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduler.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public <T> void scheduleOnce(String key, Duration delay, T message, Consumer<T> messageHandler) {
        cancelTimer(key);
        ScheduledFuture<?> timer = scheduler.schedule(
                () -> messageHandler.accept(message),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
        timers.put(key, timer);
    }

    @Override
    public void scheduleRepeatedly(String key, Duration initialDelay, Duration interval, Object message, Consumer<Object> messageHandler) {
        cancelTimer(key);
        ScheduledFuture<?> timer = scheduler.scheduleAtFixedRate(
                () -> messageHandler.accept(message),
                initialDelay.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        timers.put(key, timer);
    }

    @Override
    public void cancelTimer(String key) {
        ScheduledFuture<?> timer = timers.remove(key);
        if (timer != null) {
            logger.debug("Cancelling timer: {}", key);
            timer.cancel(false);
        }
    }

    @Override
    public void shutdown() throws InterruptedException {
        logger.info("Shutting down scheduler");
        // 取消所有计时器
        timers.values().forEach(timer -> timer.cancel(true));
        timers.clear();

        // 关闭调度器
        scheduler.shutdown();
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.info("Forcing shutdown of scheduler");
            scheduler.shutdownNow();
        }
    }
}