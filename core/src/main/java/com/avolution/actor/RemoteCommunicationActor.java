package com.avolution.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ActorSelection;
import akka.actor.ReceiveTimeout;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class RemoteCommunicationActor extends AbstractActor {

    private String remoteAddress;
    private ActorSelection remoteActor;

    public RemoteCommunicationActor(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.remoteActor = getContext().actorSelection(remoteAddress);
        getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, message -> {
                    remoteActor.tell(message, getSelf());
                })
                .match(ReceiveTimeout.class, timeout -> {
                    System.out.println("No response from remote actor, retrying...");
                    remoteActor.tell("Ping", getSelf());
                })
                .build();
    }

    public static Props props(String remoteAddress) {
        return Props.create(RemoteCommunicationActor.class, remoteAddress);
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.remoteActor = getContext().actorSelection(remoteAddress);
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }
}
