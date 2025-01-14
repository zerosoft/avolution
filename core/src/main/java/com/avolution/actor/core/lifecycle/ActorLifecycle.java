package com.avolution.actor.core.lifecycle;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;
/**
 * 整体生命周期状态管理
 * 协调外部钩子和内部钩子的调用顺序
 * 维护 Actor 的当前状态
 */
public class ActorLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ActorLifecycle.class);

    private volatile LifecycleState state = LifecycleState.NEW;
    // Actor上下文
    private final ActorContext context;
    private final ActorLifecycleHook lifecycleHook;
    private final InternalLifecycleHook internalHook;

    public ActorLifecycle(ActorContext context, UnTypedActor<?> unTypedActor) {
        this.context = context;
        this.lifecycleHook = unTypedActor.getTypedActor();
        this.internalHook = new ActorContextInternalLifecycleHook(context, this);
    }

    /**
     * 启动Actor
     */
    public void start() {
        if (state == LifecycleState.NEW) {
            try {
                state = LifecycleState.STARTING;
                
                // 执行用户定义的前置钩子
                if (!lifecycleHook.preStart()) {
                    throw new ActorInitializationException("PreStart hook returned false");
                }
                
                // 执行内部启动逻辑
                if (!internalHook.executeStart()) {
                    throw new ActorInitializationException("Internal start execution failed");
                }
                
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
    public void stop(CompletableFuture<Void> stopFuture) {
        if (state == LifecycleState.RUNNING) {
            try {
                state = LifecycleState.STOPPING;
                
                // 执行用户定义的前置钩子
                if (!lifecycleHook.preStop()) {
                    logger.warn("PreStop hook returned false for actor: {}", context.getPath());
                }
                
                // 执行内部停止逻辑
                if (!internalHook.executeStop()) {
                    logger.warn("Internal stop execution failed for actor: {}", context.getPath());
                }
                
                state = LifecycleState.STOPPED;
                stopFuture.complete(null);
                logger.debug("Actor stopped: {}", context.getPath());
            } catch (Exception e) {
                state = LifecycleState.FAILED;
                stopFuture.completeExceptionally(e);
                logger.error("Failed to stop actor: {}", context.getPath(), e);
            }
        }
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