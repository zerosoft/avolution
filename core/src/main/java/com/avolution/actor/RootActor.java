package com.avolution.actor;

import com.avolution.actor.core.Actor;
import com.avolution.actor.core.ActorSystem;

public class RootActor extends Actor {

    private ActorSystem actorSystem;

    public RootActor(ActorSystem actorSystem) {
        this.actorSystem=actorSystem;
    }

    @Override
    protected void receive(Object message) {
        switch (message){
            case null, default -> {
                System.out.println("Unknown message received: "+message);
            }
        }
    }
}
