package com.avolution.actor.core.lifecycle;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ActorLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ActorLifecycle.class);

    private volatile LifecycleState state = LifecycleState.NEW;

    private final CompletableFuture<Void> terminationFuture = new CompletableFuture<>();

    private final ActorContext context;

    public ActorLifecycle(ActorContext context) {
        this.context = context;
    }

    /**
     * 启动Actor
     */
    public void start() {
        if (state == LifecycleState.NEW) {
            try {
                state = LifecycleState.STARTING;

                context.getSelf().initialize();

                state = LifecycleState.RUNNING;
                logger.debug("Actor started: {}", context.getPath());
            } catch (Exception e) {
                state = LifecycleState.FAILED;
                logger.error("Failed to start actor: {}", context.getPath(), e);
                throw new ActorInitializationException("Failed to start actor", e);
            }
        }
    }

    /**
     * 停止Actor
     * @return
     */
    public CompletableFuture<Void> stop() {
        if (state == LifecycleState.RUNNING || state == LifecycleState.SUSPENDED) {
            try {
                state = LifecycleState.STOPPING;

                cleanupResources();

                state = LifecycleState.STOPPED;
                terminationFuture.complete(null);
                logger.debug("Actor stopped: {}", context.getPath());
            } catch (Exception e) {
                state = LifecycleState.FAILED;
                terminationFuture.completeExceptionally(e);
                logger.error("Failed to stop actor: {}", context.getPath(), e);
            }
        }
        return terminationFuture;
    }

    /**
     * 重启Actor
     */
    public void restart() {
        try {
            state = LifecycleState.RESTARTING;
            cleanupResources();
            state = LifecycleState.RUNNING;
            logger.debug("Actor restarted: {}", context.getPath());
        } catch (Exception e) {
            state = LifecycleState.FAILED;
            logger.error("Failed to restart actor: {}", context.getPath(), e);
            throw new ActorInitializationException("Failed to restart actor", e);
        }
    }


    // 1. 暂停Actor
    public void suspend() {
        if (state == LifecycleState.RUNNING) {
            state = LifecycleState.SUSPENDED;
            context.getMailbox().suspend();
            logger.debug("Actor suspended: {}", context.getPath());
        }
    }
    // 2. 恢复Actor
    public void resume() {
        if (state == LifecycleState.SUSPENDED) {
            state = LifecycleState.RUNNING;
            context.getMailbox().resume();
            logger.debug("Actor resumed: {}", context.getPath());
        }
    }


    private void cleanupResources() {
        try {
            context.getMailbox().clear();
        } catch (Exception e) {
            logger.warn("Failed to clear mailbox for actor: {}", context.getPath(), e);
        }
    }

    public LifecycleState getState() {
        return state;
    }

    public boolean isTerminated() {
        return state == LifecycleState.STOPPED || state == LifecycleState.FAILED;
    }

    /**
     * 立即终止生命周期
     */
    public void terminate() {

    }

    public boolean isStopping() {
        return state == LifecycleState.STOPPING;
    }
}