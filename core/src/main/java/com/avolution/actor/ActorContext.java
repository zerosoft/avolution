package com.avolution.actor;

public interface ActorContext {

    ActorRef actorOf(Props props, String name);

    void stop(ActorRef actor);

    ActorRef getSelf();

    ActorRef getSender();

    ActorRef parent();

    void setSender(ActorRef sender);
}
