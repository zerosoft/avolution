package com.avolution.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.annotation.OnReceive;

import java.util.concurrent.TimeUnit;

public class HelloActor extends AbstractActor<HelloActorMessage> {

    @OnReceive(HelloActorMessage.Hello.class)
    public void handleHelloMessage(HelloActorMessage.Hello message) {
        System.out.println("Hello, Actor! " + message);
    }

    @OnReceive(HelloActorMessage.World.class)
    public void handleWorldMessage(HelloActorMessage.World message) {
        System.out.println("World, Actor! " + message);
        getSender().tell("OK", getSelfRef());

        getContext().getScheduler().schedule(()->{
            System.out.println("Hello, Actor! timer  " + message);
        },1000, TimeUnit.MILLISECONDS);
    }

    @OnReceive(HelloActorMessage.Timer.class)
    public void handleWorldMessage(HelloActorMessage.Timer message) {
        System.out.println("World, Actor! " + message);

        getContext().getScheduler().schedule(()->{
            System.out.println("Hello, Actor! timer  " + message);
        },1000, TimeUnit.MILLISECONDS);
    }

    @OnReceive(HelloActorMessage.Terminate.class)
    public void handleStopMessage(HelloActorMessage.Terminate message) {
        System.out.println("Stop, Actor! " + message);
//        getContext().stopSelf(getSelfRef());
    }


}
