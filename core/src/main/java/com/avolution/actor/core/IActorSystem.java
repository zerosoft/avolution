package com.avolution.actor.core;

import com.avolution.actor.Props;

public interface IActorSystem {

    String getName();

    ActorRef actorOf(Props props, String name);

    void stop(ActorRef actor);

    void terminate();
}
