package com.avolution.actor.deadLetter;

import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.annotation.OnReceive;

public class StringActor extends UnTypedActor<String> {

    @OnReceive(String.class)
    private void onMessage(String msg) {
        System.out.println("Received message: " + msg);
    }

}
