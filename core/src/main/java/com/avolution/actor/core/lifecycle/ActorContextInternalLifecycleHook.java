package com.avolution.actor.core.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.context.ActorContext;

/**
 * ActorContextInternalLifecycleHook 类实现了 InternalLifecycleHook 接口，
 * 用于在 Actor 的生命周期中执行内部钩子操作。
 * 它通过 ActorContext 和 ActorLifecycle 来管理 Actor 的状态和行为。
 */
public class ActorContextInternalLifecycleHook implements InternalLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(ActorContextInternalLifecycleHook.class);

    // Actor 上下文
    private final ActorContext actorContext;

    // Actor 生命周期管理器
    private final ActorLifecycle actorLifecycle;

    /**
     * 构造函数，初始化 ActorContextInternalLifecycleHook 实例。
     *
     * @param actorContext Actor 上下文
     * @param lifecycle    Actor 生命周期管理器
     */
    public ActorContextInternalLifecycleHook(ActorContext actorContext, ActorLifecycle lifecycle) {
        this.actorContext = actorContext;
        this.actorLifecycle = lifecycle;
    }

    /**
     * 执行启动操作。
     * 该方法会调用 ActorLifecycle 的 start 方法，并恢复 Actor 的邮箱。
     *
     * @return 如果启动成功，返回 true；否则返回 false
     */
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

    /**
     * 执行停止操作。
     * 该方法会暂停 Actor 的邮箱，并调用 ActorLifecycle 的 stop 方法。
     *
     * @return 如果停止成功，返回 true；否则返回 false
     */
    @Override
    public boolean executeStop() {
        try {
            // 暂停邮箱
            actorContext.getMailbox().suspend();
            return actorLifecycle.stop();
        } catch (Exception e) {
            logger.error("Failed to execute stop", e);
            return false;
        }
    }

    /**
     * 执行重启操作。
     * 该方法会调用 ActorLifecycle 的 restart 方法。
     *
     * @param reason 重启的原因
     * @return 如果重启成功，返回 true；否则返回 false
     */
    @Override
    public boolean executeRestart(Throwable reason) {
        try {
            return actorLifecycle.restart();
        } catch (Exception e) {
            logger.error("Failed to execute restart", e);
            return false;
        }
    }

    /**
     * 执行暂停操作。
     * 该方法会暂停 Actor 的邮箱。
     *
     * @return 如果暂停成功，返回 true；否则返回 false
     */
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

    /**
     * 执行恢复操作。
     * 该方法会恢复 Actor 的邮箱。
     *
     * @return 如果恢复成功，返回 true；否则返回 false
     */
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

    /**
     * 获取当前的生命周期状态。
     *
     * @return 当前的生命周期状态
     */
    @Override
    public LifecycleState getCurrentState() {
        return actorLifecycle.getState();
    }
}