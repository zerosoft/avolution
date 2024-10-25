package com.avolution.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.OneForOneStrategy;
import akka.actor.Terminated;
import akka.japi.pf.DeciderBuilder;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SupervisorActor extends AbstractActor {

    private final List<ActorRef> childActors = new ArrayList<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Props.class, props -> {
                    ActorRef child = getContext().actorOf(props);
                    getContext().watch(child);
                    childActors.add(child);
                })
                .match(Terminated.class, terminated -> {
                    childActors.remove(terminated.getActor());
                })
                .build();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(
                10,
                Duration.create(1, TimeUnit.MINUTES),
                DeciderBuilder
                        .match(Exception.class, e -> SupervisorStrategy.restart())
                        .build()
        );
    }

    public void createChildActor(Props props) {
        ActorRef child = getContext().actorOf(props);
        getContext().watch(child);
        childActors.add(child);
    }

    public List<ActorRef> getChildActors() {
        return new ArrayList<>(childActors);
    }
}
