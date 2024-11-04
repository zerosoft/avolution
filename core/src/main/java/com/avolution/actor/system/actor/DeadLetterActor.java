package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadLetterActor extends AbstractActor<Object> {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);
    
    @Override
    public void onReceive(Object message) {

    }
}