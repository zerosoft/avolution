package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadLetterActor extends AbstractActor<IDeadLetterActorMessage> {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);
    
    @Override
    public void onReceive(IDeadLetterActorMessage message) {

    }
}