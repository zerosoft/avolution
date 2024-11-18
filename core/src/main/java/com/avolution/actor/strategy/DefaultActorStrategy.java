package com.avolution.actor.strategy;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.*;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultActorStrategy<T> implements ActorStrategy<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultActorStrategy.class);
    private final SupervisorStrategy supervisorStrategy = new DefaultSupervisorStrategy();

    @Override
    public void beforeMessageHandle(Envelope<T> message, AbstractActor<T> self) {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing message: {} in actor: {}",
                    message.getMessage().getClass().getSimpleName(), self.path());
        }
    }


    @Override
    public void afterMessageHandle(Envelope<T> message, AbstractActor<T> self, boolean success) {
        if (!success && logger.isWarnEnabled()) {
            logger.warn("Message processing failed: {} in actor: {}",
                    message.getMessage().getClass().getSimpleName(), self.path());
        }
    }

    @Override
    public SupervisorStrategy getSupervisionStrategy() {
        return supervisorStrategy;
    }
}