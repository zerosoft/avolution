package com.avolution.actor;

public class RootActor extends Actor{

    private ActorSystem actorSystem;

    public RootActor(ActorSystem actorSystem) {
        this.actorSystem=actorSystem;
    }

    @Override
    protected void receive(Object message) {
        actorSystem.processMessage(message);
    }
}
