package com.avolution.actor.core;

import java.time.Duration;
import java.util.function.Consumer;

public interface ActorScheduler extends IScheduler{

    <T> void scheduleOnce(String key, Duration delay, T message, Consumer<T> messageHandler);

    void scheduleRepeatedly(String key, Duration initialDelay, Duration interval, Object message, Consumer<Object> messageHandler);

    void cancelTimer(String key);

    void shutdown() throws InterruptedException;
}
