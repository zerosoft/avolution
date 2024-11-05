package com.avolution.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.Props;

import java.util.concurrent.TimeUnit;

public class HelloActor extends AbstractActor<String> {

    @Override
    public void onReceive(String message) {
        try {
            TimeUnit.MILLISECONDS.sleep(100L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Thread.currentThread().toString()+" Hello " + message);
//        getSender().tell("Hello " + message, this);
//        getSender().tell("Hello " + message, this);
        ActorRef actorRef = getContext().actorOf(Props.create(HelloActor.class), "hello-actor-" + message);
        actorRef.tell("Hello Two" + message, this);
    }
}
