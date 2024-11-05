package com.avolution.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.annotation.OnReceive;

public class HelloActor extends AbstractActor<HelloActorMessage> {

    @OnReceive(HelloActorMessage.Hello.class)
    public void handleHelloMessage(HelloActorMessage.Hello message) {
        System.out.println("Hello, Actor! " + message);
    }

    @OnReceive(HelloActorMessage.World.class)
    public void handleWorldMessage(HelloActorMessage.World message) {
        System.out.println("World, Actor! " + message);
    }
}
