package com.avolution.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class ActorSystemManager {

    private ActorSystem actorSystem;
    private ActorRef rootActor;

    public ActorSystemManager() {
        initializeActorSystem();
    }

    private void initializeActorSystem() {
        actorSystem = ActorSystem.create("AvolutionActorSystem");
        rootActor = actorSystem.actorOf(Props.create(SupervisorActor.class), "rootActor");
    }

    public void stopActorSystem() {
        if (actorSystem != null) {
            actorSystem.terminate();
        }
    }

    public void restartActorSystem() {
        stopActorSystem();
        initializeActorSystem();
    }

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ActorRef getRootActor() {
        return rootActor;
    }
}
