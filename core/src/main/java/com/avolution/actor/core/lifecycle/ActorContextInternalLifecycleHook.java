package com.avolution.actor.core.lifecycle;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.context.ActorContext;

public class ActorContextInternalLifecycleHook implements InternalLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(ActorContextInternalLifecycleHook.class);

    private final ActorContext actorContext;
    private final ActorLifecycle actorLifecycle;

    public ActorContextInternalLifecycleHook(ActorContext actorContext, ActorLifecycle lifecycle) {
        this.actorContext = actorContext;
        this.actorLifecycle = lifecycle;
    }

    @Override
    public boolean executeStart() {
        try {
            actorLifecycle.start();
            actorContext.getMailbox().resume();
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute start", e);
            return false;
        }
    }

    @Override
    public boolean executeStop() {
        try {
            actorContext.getMailbox().suspend();
            actorLifecycle.stop(new CompletableFuture<>());
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute stop", e);
            return false;
        }
    }

    @Override
    public boolean executeRestart(Throwable reason) {
        try {
            actorLifecycle.restart();
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute restart", e);
            return false;
        }
    }

    @Override
    public boolean executeSuspend() {
        try {
            actorContext.getMailbox().suspend();
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute suspend", e);
            return false;
        }
    }

    @Override
    public boolean executeResume() {
        try {
            actorContext.getMailbox().resume();
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute resume", e);
            return false;
        }
    }

    @Override
    public LifecycleState getCurrentState() {
        return actorLifecycle.getState();
    }
}
