package com.avolution.monitoring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class PerformanceMetricsCollector {

    private final ActorSystem actorSystem;
    private final long collectionInterval;
    private Cancellable cancellable;

    public PerformanceMetricsCollector(ActorSystem actorSystem, long collectionInterval) {
        this.actorSystem = actorSystem;
        this.collectionInterval = collectionInterval;
    }

    public void start() {
        cancellable = actorSystem.scheduler().schedule(
                Duration.Zero(),
                Duration.create(collectionInterval, TimeUnit.MILLISECONDS),
                this::collectMetrics,
                actorSystem.dispatcher()
        );
    }

    public void stop() {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    private void collectMetrics() {
        // Implement the logic to collect and report metrics for actors
        // For example, you can collect metrics like message processing time, message queue size, etc.
    }

    public static Props props(ActorSystem actorSystem, long collectionInterval) {
        return Props.create(PerformanceMetricsCollector.class, () -> new PerformanceMetricsCollector(actorSystem, collectionInterval));
    }
}
