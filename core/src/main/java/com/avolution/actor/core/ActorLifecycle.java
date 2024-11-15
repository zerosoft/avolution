package com.avolution.actor.core;

import com.avolution.actor.exception.ActorInitializationException;
import com.avolution.actor.exception.ActorRestartException;
import com.avolution.actor.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ActorLifecycle {
    private final AtomicReference<LifecycleState> lifecycleState = new AtomicReference<>(LifecycleState.NEW);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // 核心生命周期方法
    public final void start() {
        if (lifecycleState.compareAndSet(LifecycleState.NEW, LifecycleState.STARTING)) {
            try {
                logger.info("Starting actor: {}", getClass().getName());
                doStart();
                lifecycleState.set(LifecycleState.RUNNING);
            } catch (Exception e) {
                lifecycleState.set(LifecycleState.STOPPED);
                throw new ActorInitializationException("Failed to start actor", e);
            }
        }
    }

    public final CompletableFuture<Void> stop() {
        if (!lifecycleState.compareAndSet(LifecycleState.RUNNING, LifecycleState.STOPPING)) {
            logger.info("Actor is not running, skipping stop");
            return CompletableFuture.completedFuture(null);
        }
        return doStop().whenComplete((v, e) -> lifecycleState.set(LifecycleState.STOPPED));

    }

    public final void restart(Throwable reason) throws ActorRestartException {
        if (lifecycleState.compareAndSet(LifecycleState.RUNNING, LifecycleState.RESTARTING)) {
            try {
                logger.info("Restarting actor: {}", getClass().getName());
                doRestart(reason);
                lifecycleState.set(LifecycleState.RUNNING);
            } catch (Exception e) {
                lifecycleState.set(LifecycleState.STOPPED);
                throw new ActorRestartException("Failed to restart actor", e);
            }
        }
    }

    public final void forceStop() {
        logger.info("Force stopping actor: {}", getClass().getName());
        if (lifecycleState.get() == LifecycleState.STOPPED) {
            return;
        }

        try {
            lifecycleState.set(LifecycleState.STOPPING);

            // 中断当前处理
            interruptCurrentProcessing();

            try {
                onPreStop();
                onPostStop();
            } catch (Exception e) {
                logger.error("Error in stop callbacks during force stop", e);
            }

            try {
                doCleanup();
            } catch (Exception e) {
                logger.error("Error cleaning up resources during force stop", e);
            }
        } catch (Exception e) {
            logger.error("Error during force stop", e);
        } finally {
            lifecycleState.set(LifecycleState.STOPPED);
            isShuttingDown.set(true);
        }
    }


    public LifecycleState getLifecycleState() {
        return lifecycleState.get();
    }

    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }

    // 模板方法
    protected abstract CompletableFuture<Void> waitForMessageProcessing();
    protected abstract void interruptCurrentProcessing();
    protected abstract void doCleanup();


    protected abstract void doStart();
    protected abstract void doRestart(Throwable reason);
    protected abstract CompletableFuture<Void> doStop();
    // 生命周期回调
    protected abstract void onPreStart();
    protected abstract void onPostStart();
    protected abstract void onPreStop();
    protected abstract void onPostStop();
    protected abstract void onPreRestart(Throwable reason);
    protected abstract void onPostRestart(Throwable reason);
}