package com.avolution.actor.strategy;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Signal;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.OneForOneStrategy;

import java.time.Duration;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Signal;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.Directive;
import com.avolution.actor.supervision.OneForOneStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class DefaultActorStrategy<T> implements ActorStrategy<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultActorStrategy.class);
    private final com.avolution.actor.supervision.SupervisorStrategy supervisorStrategy;

    public DefaultActorStrategy() {
        this.supervisorStrategy = new OneForOneStrategy(
                3, Duration.ofMinutes(1),
                SupervisorStrategy.defaultDecider()
        );
    }

    @Override
    public void beforeMessageHandle(Envelope<T> message, AbstractActor<T> self) {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing message: {} in actor: {}",
                    message.getMessage().getClass().getSimpleName(), self.path());
        }
    }

    @Override
    public void handleMessage(Envelope<T> message, AbstractActor<T> self) {
        if (message.getMessage() instanceof Signal signal) {
            signal.handle(self);
        } else {
            self.onReceive(message.getMessage());
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
    public void handleFailure(Throwable cause, Envelope<T> message, AbstractActor<T> self) {
        Directive directive = supervisorStrategy.handle(cause);
        switch (directive) {
            case RESUME -> self.getContext().resume();
            case RESTART -> self.getContext().restart(cause);
            case RESTART_ALL -> restartAllChildren(self);
            case STOP -> self.getContext().stop();
            case ESCALATE -> self.getContext().escalate(cause);
        }
    }

    @Override
    public void onPreStart(AbstractActor<T> self) {
        if (logger.isDebugEnabled()) {
            logger.debug("Actor starting: {}", self.path());
        }
    }

    @Override
    public void onPostStop(AbstractActor<T> self) {
        if (logger.isDebugEnabled()) {
            logger.debug("Actor stopped: {}", self.path());
        }
    }

    @Override
    public void onPreRestart(Throwable reason, AbstractActor<T> self) {
        if (logger.isDebugEnabled()) {
            logger.debug("Actor restarting due to: {} in actor: {}",
                    reason.getClass().getSimpleName(), self.path());
        }
        // 停止所有子Actor
        self.getContext().getChildren().values()
                .forEach(child -> self.getContext().stop(child));
    }

    @Override
    public void onPostRestart(Throwable reason, AbstractActor<T> self) {
        if (logger.isDebugEnabled()) {
            logger.debug("Actor restarted: {}", self.path());
        }
        onPreStart(self);
    }

    private void restartAllChildren(AbstractActor<T> self) {
        self.getContext().getChildren().values()
                .forEach(child -> self.getContext().restart(null));
    }
}