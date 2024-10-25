package com.avolution.monitoring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ActorStateMonitor {

    private final ActorSystem actorSystem;
    private final long monitoringInterval;
    private Cancellable cancellable;

    public ActorStateMonitor(ActorSystem actorSystem, long monitoringInterval) {
        this.actorSystem = actorSystem;
        this.monitoringInterval = monitoringInterval;
    }

    public void start() {
        cancellable = actorSystem.scheduler().schedule(
                Duration.Zero(),
                Duration.create(monitoringInterval, TimeUnit.MILLISECONDS),
                this::monitorActorStates,
                actorSystem.dispatcher()
        );
    }

    public void stop() {
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    private void monitorActorStates() {
        // Implement the logic to track and report actor states
        // For example, you can track actor lifecycle events, message processing status, etc.
    }

    public static Props props(ActorSystem actorSystem, long monitoringInterval) {
        return Props.create(ActorStateMonitor.class, () -> new ActorStateMonitor(actorSystem, monitoringInterval));
    }
}
