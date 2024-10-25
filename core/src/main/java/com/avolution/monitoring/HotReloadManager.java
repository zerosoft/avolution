package com.avolution.monitoring;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class HotReloadManager {

    private final ActorSystem actorSystem;

    public HotReloadManager(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    public void reloadActor(String actorName, Props newProps) {
        ActorRef actorRef = actorSystem.actorFor(actorName);
        if (actorRef != null) {
            Future<Terminated> terminationFuture = Patterns.gracefulStop(actorRef, Duration.create(5, TimeUnit.SECONDS));
            terminationFuture.onComplete(result -> {
                if (result.isSuccess()) {
                    actorSystem.actorOf(newProps, actorName);
                } else {
                    System.err.println("Failed to stop actor: " + actorName);
                }
            }, actorSystem.dispatcher());
        } else {
            System.err.println("Actor not found: " + actorName);
        }
    }

    public void configureHotReloadSettings(long timeout, TimeUnit timeUnit) {
        // Implement logic to configure hot-reload settings
        // For example, you can set the timeout for actor termination
    }
}
