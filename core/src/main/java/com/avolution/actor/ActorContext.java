package com.avolution.actor;

public interface ActorContext {

    ActorRef actorOf(Props props, String name);

    void stop(ActorRef actor);

    ActorRef self();

    ActorRef sender();

    ActorRef parent();
}
