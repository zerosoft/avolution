package com.avolution.actor.core.lifecycle;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorInitializationException;

/**
 * ActorLifecycle 类负责管理 Actor 的生命周期状态。
 * 它协调外部钩子和内部钩子的调用顺序，并维护 Actor 的当前状态。
 */
public class ActorLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ActorLifecycle.class);

    // Actor 的当前生命周期状态
    private volatile LifecycleState state = LifecycleState.NEW;

    // Actor 上下文
    private final ActorContext context;

    // Actor 生命周期钩子
    private final ActorLifecycleHook lifecycleHook;

    /**
     * 构造函数，初始化 ActorLifecycle 实例。
     *
     * @param context     Actor 上下文
     * @param unTypedActor 未类型化的 Actor 实例
     */
    public ActorLifecycle(ActorContext context, UnTypedActor<?> unTypedActor) {
        this.context = context;
        this.lifecycleHook = unTypedActor.getTypedActor();
    }

    /**
     * 启动 Actor。
     * 该方法会执行用户定义的前置钩子，并将 Actor 状态从 NEW 转换为 RUNNING。
     * 如果启动过程中发生异常，Actor 状态将变为 FAILED。
     */
    public void start() {
        if (state == LifecycleState.NEW) {
            try {
                state = LifecycleState.STARTING;

                // 执行用户定义的前置钩子
                if (!lifecycleHook.preStart()) {
                    throw new ActorInitializationException("PreStart hook returned false");
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
     * 停止 Actor。
     * 该方法会执行用户定义的前置钩子，并将 Actor 状态从 RUNNING 转换为 STOPPED。
     * 如果停止过程中发生异常，Actor 状态将变为 FAILED。
     *
     */
    public boolean stop() {
        if (state == LifecycleState.RUNNING) {
            try {
                state = LifecycleState.STOPPING;

                // 执行用户定义的前置钩子
                if (!lifecycleHook.preStop()) {
                    logger.warn("PreStop hook returned false for actor: {}", context.getPath());
                }

                state = LifecycleState.STOPPED;
                logger.debug("Actor stopped: {}", context.getPath());
                return true;
            } catch (Exception e) {
                state = LifecycleState.FAILED;
                logger.error("Failed to stop actor: {}", context.getPath(), e);
                return false;
            }
        }
        return false;
    }

    /**
     * 重启 Actor。
     * 该方法会执行用户定义的前置和后置重启钩子，并将 Actor 状态从 RUNNING 转换为 RESTARTING，最后恢复为 RUNNING。
     * 如果重启过程中发生异常，Actor 状态将变为 FAILED。
     */
    public boolean restart() {
        try {
            state = LifecycleState.RESTARTING;
            Throwable cause = new Throwable("Restart");
            lifecycleHook.preRestart(cause);

            lifecycleHook.postRestart(cause);
            state = LifecycleState.RUNNING;
            logger.debug("Actor restarted: {}", context.getPath());
            return true;
        } catch (Exception e) {
            state = LifecycleState.FAILED;
            logger.error("Failed to restart actor: {}", context.getPath(), e);
            throw new ActorInitializationException("Failed to restart actor", e);
        }
    }

    /**
     * 暂停 Actor。
     * 该方法会将 Actor 状态从 RUNNING 转换为 SUSPENDED，并暂停 Actor 的邮箱。
     */
    public void suspend() {
        if (state == LifecycleState.RUNNING) {
            state = LifecycleState.SUSPENDED;
            context.getMailbox().suspend();
            logger.debug("Actor suspended: {}", context.getPath());
        }
    }

    /**
     * 恢复 Actor。
     * 该方法会将 Actor 状态从 SUSPENDED 转换为 RUNNING，并恢复 Actor 的邮箱。
     */
    public void resume() {
        if (state == LifecycleState.SUSPENDED) {
            state = LifecycleState.RUNNING;
            context.getMailbox().resume();
            logger.debug("Actor resumed: {}", context.getPath());
        }
    }

    /**
     * 获取 Actor 的当前状态。
     *
     * @return 当前的生命周期状态
     */
    public LifecycleState getState() {
        return state;
    }

    /**
     * 检查 Actor 是否已终止。
     *
     * @return 如果 Actor 状态为 STOPPED 或 FAILED，则返回 true，否则返回 false
     */
    public boolean isTerminated() {
        return state == LifecycleState.STOPPED || state == LifecycleState.FAILED;
    }

    /**
     * 检查 Actor 是否正在停止。
     *
     * @return 如果 Actor 状态为 STOPPING，则返回 true，否则返回 false
     */
    public boolean isStopping() {
        return state == LifecycleState.STOPPING;
    }
}