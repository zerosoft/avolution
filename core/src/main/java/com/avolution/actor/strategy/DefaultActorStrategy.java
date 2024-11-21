package com.avolution.actor.strategy;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.exception.ActorException;
import com.avolution.actor.message.*;
import com.avolution.actor.metrics.ActorMetricsCollector;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultActorStrategy implements ActorStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultActorStrategy.class);
    private final SupervisorStrategy supervisorStrategy;
    private final ActorMetricsCollector metricsCollector;
    
    public DefaultActorStrategy() {
        this(new DefaultSupervisorStrategy());
    }
    
    public DefaultActorStrategy(SupervisorStrategy supervisorStrategy) {
        this.supervisorStrategy = supervisorStrategy;
        this.metricsCollector = new ActorMetricsCollector("");
    }
    
    @Override
    public void beforeMessageHandle(Envelope message, AbstractActor<?> actor) {
        metricsCollector.incrementMessageCount();
        metricsCollector.recordMessageReceived(actor.path());
        logger.debug("Processing message {} in actor {}", message.getMessage().getClass().getSimpleName(), actor.path());
    }
    
    @Override
    public void afterMessageHandle(Envelope message, AbstractActor<?> actor, boolean success) {
        if (success) {
            metricsCollector.recordMessageProcessed(actor.path());
            logger.debug("Successfully processed message {} in actor {}", 
                message.getMessage().getClass().getSimpleName(), actor.path());
        } else {
            metricsCollector.recordMessageFailed(actor.path());
            logger.debug("Failed to process message {} in actor {}", 
                message.getMessage().getClass().getSimpleName(), actor.path());
        }
    }
    
    @Override
    public void handleFailure(Throwable cause, Envelope message, AbstractActor<?> actor) {
        logger.error("Actor {} failed to process message: {}", actor.path(), cause.getMessage());
        metricsCollector.recordFailure(actor.path(), cause);
        
        try {
            Directive directive = supervisorStrategy.handle(cause);
            handleDirectiveWithMetrics(directive, cause, actor, message);
        } catch (Exception e) {
            logger.error("Error handling failure in actor {}", actor.path(), e);
        }
    }
    
    private void handleDirectiveWithMetrics(Directive directive, Throwable cause, 
            AbstractActor<?> actor, Envelope message) {
        switch (directive) {
            case RESUME -> {
                metricsCollector.recordResume(actor.path());
                actor.getContext().resume();
            }
            case RESTART -> {
                metricsCollector.recordRestart(actor.path());
            }
            case STOP -> {
                metricsCollector.recordStop(actor.path());
            }
            case ESCALATE -> {
                metricsCollector.recordEscalation(actor.path());
            }
        }
    }
    
    @Override
    public void onPreStart(AbstractActor<?> actor) {
        metricsCollector.recordLifecycleEvent(actor.path(), "preStart");
        logger.debug("Actor {} starting", actor.path());
    }
    
    @Override
    public void onPreStop(AbstractActor<?> actor) {
        metricsCollector.recordLifecycleEvent(actor.path(), "preStop");
        logger.debug("Actor {} stopping", actor.path());
    }
    
    @Override
    public void onPostStop(AbstractActor<?> actor) {
        metricsCollector.recordLifecycleEvent(actor.path(), "postStop");
        logger.debug("Actor {} stopped", actor.path());
    }
    
    @Override
    public void onPreRestart(AbstractActor<?> actor, Throwable reason) {
        metricsCollector.recordLifecycleEvent(actor.path(), "preRestart");
        logger.debug("Actor {} restarting due to {}", actor.path(), reason.getMessage());
    }
    
    @Override
    public void onPostRestart(AbstractActor<?> actor, Throwable reason) {
        metricsCollector.recordLifecycleEvent(actor.path(), "postRestart");
        logger.debug("Actor {} restarted after {}", actor.path(), reason.getMessage());
    }
    
    @Override
    public SupervisorStrategy getSupervisionStrategy() {
        return supervisorStrategy;
    }
}