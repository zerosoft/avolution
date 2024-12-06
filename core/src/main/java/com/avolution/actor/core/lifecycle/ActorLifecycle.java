package com.avolution.actor.core.lifecycle;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.avolution.actor.core.TypedActor;
import com.avolution.actor.core.UnTypedActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
/**
 * Actor 生命周期
 */
public class ActorLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ActorLifecycle.class);

    private volatile LifecycleState state = LifecycleState.NEW;
    // Actor上下文
    private final ActorContext context;

    private TypedActor typedActor;

    public ActorLifecycle(ActorContext context, UnTypedActor typedActor) {
        this.context = context;
    }

    /**
     * 启动Actor
     */
    public void start() {
        if (state == LifecycleState.NEW) {
            try {
                state = LifecycleState.STARTING;
                // 执行Actor启动前钩子

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
    public CompletableFuture<Void> stop(CompletableFuture<Void> stopFuture) {
        if (state != LifecycleState.RUNNING && state != LifecycleState.SUSPENDED) {
            stopFuture.complete(null);
            return stopFuture;
        }
        logger.debug("Stopping actor: {}", context.getPath());
        try {
            state = LifecycleState.STOPPING;
            // 1. 停止子Actor
            stopChildren()
                    .thenRun(() -> {
                        // 2. 执行停止回调
//                        internalLifecycleHook.executePostStop();

                        state = LifecycleState.STOPPED;
                        stopFuture.complete(null);
                    })
                    .exceptionally(e -> {
                        state = LifecycleState.FAILED;
                        stopFuture.completeExceptionally(e);
                        return null;
                    });

        } catch (Exception e) {
            state = LifecycleState.FAILED;
            stopFuture.completeExceptionally(e);
        }
        return stopFuture;
    }

    private CompletableFuture<Void> stopChildren() {
        List<CompletableFuture<Void>> childStopFutures = context.getChildren()
                .values()
                .stream()
                .map(child -> context.stop(child))
                .toList();
        return CompletableFuture.allOf(childStopFutures.toArray(new CompletableFuture[0]));
    }

    /**
     * 重启Actor
     */
    public void restart() {
        try {
            state = LifecycleState.RESTARTING;

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

    public LifecycleState getState() {
        return state;
    }

    /**
     * 是否已终止
     * @return
     */
    public boolean isTerminated() {
        return state == LifecycleState.STOPPED || state == LifecycleState.FAILED;
    }

    /**
     * 立即终止生命周期
     */
    public void terminate() {
    }

    /**
     * 是否正在启动
     * @return
     */
    public boolean isStopping() {
        return state == LifecycleState.STOPPING;
    }
}