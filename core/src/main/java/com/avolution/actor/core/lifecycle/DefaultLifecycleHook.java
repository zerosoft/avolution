package com.avolution.actor.core.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
/**
 * 默认生命周期钩子
 */
public class DefaultLifecycleHook implements InternalLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLifecycleHook.class);

    @Override
    public void aroundPreStart(ActorContext context) {
        try {
            context.getSelf().initialize();

            logger.debug("Actor starting: {}", context.getPath());
        } catch (Exception e) {
            logger.error("Failed to start actor: {}", context.getPath(), e);
            throw new ActorInitializationException("Failed to start actor", e);
        }
    }

    @Override
    public void aroundPostStop(ActorContext context) {
        try {
            context.getMailbox().clear();
            context.getActorSystem().unregisterActor(context.getPath());
            logger.debug("Actor stopped: {}", context.getPath());
        } catch (Exception e) {
            logger.error("Failed to stop actor: {}", context.getPath(), e);
        }
    }

    @Override
    public void aroundPreRestart(ActorContext context, Throwable reason) {
        try {
            context.getMailbox().suspend();
            context.getLifecycle().suspend();
            logger.debug("Actor pre-restarting: {}", context.getPath());
        } catch (Exception e) {
            logger.error("Failed in pre-restart: {}", context.getPath(), e);
        }
    }

    @Override
    public void aroundPostRestart(ActorContext context, Throwable reason) {
        try {
            context.getMailbox().resume();
            context.getLifecycle().resume();
            logger.debug("Actor post-restarting: {}", context.getPath());
        } catch (Exception e) {
            logger.error("Failed in post-restart: {}", context.getPath(), e);
        }
    }


}