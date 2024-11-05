package com.avolution.actor;

import com.avolution.actor.core.AbstractActor;

public class HelloActor extends AbstractActor<String> {

    @Override
    public void onReceive(Object message) {
        System.out.println("Hello " + message);
    }
}
