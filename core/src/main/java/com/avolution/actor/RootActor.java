package com.avolution.actor;

import com.avolution.actor.core.Actor;
import com.avolution.actor.core.IActorSystem;

public class RootActor extends Actor {

    private IActorSystem actorSystem;

    public RootActor(IActorSystem actorSystem) {
        this.actorSystem=actorSystem;
    }

    @Override
    protected void receive(Object message) {
    }
}
