package com.avolution.actor.deadLetter;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.annotation.OnReceive;

public class StringActor extends AbstractActor<String> {

    @OnReceive(String.class)
    private void onMessage(String msg) {
        System.out.println("Received message: " + msg);
    }

}
