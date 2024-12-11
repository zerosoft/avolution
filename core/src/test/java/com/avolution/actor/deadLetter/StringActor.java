package com.avolution.actor.deadLetter;

import com.avolution.actor.core.TypedActor;
import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.annotation.OnReceive;

public class StringActor extends TypedActor<String> {

    @Override
    protected void onReceive(String message) throws Exception {
        System.out.println("Received message: " + message);
    }
}
