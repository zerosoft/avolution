package com.avolution.actor.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface IScheduler {

    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);

}
