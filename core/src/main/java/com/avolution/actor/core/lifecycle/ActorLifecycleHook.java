package com.avolution.actor.core.lifecycle;

import java.util.function.Consumer;

import com.avolution.actor.message.Envelope;

/**
 *
 * Actor生命周期回调接口
 *
 */
/**
 * Actor生命周期钩子接口
 * 定义了Actor在其生命周期的不同阶段可以执行的回调方法
 */
public interface ActorLifecycleHook {
    /**
     * Actor启动前的预处理钩子
     * - 在Actor实例创建后、开始处理消息前调用
     * - 用于初始化资源、建立连接等准备工作
     * - 如果抛出异常，Actor将无法启动
     */
    default void preStart() {}

    /**
     * Actor重启前的预处理钩子
     * - 在Actor即将重启前调用
     * - 用于保存状态、清理资源等
     * - 重启不会创建新的Actor实例，而是重置当前实例
     *
     * @param reason 导致重启的原因(异常)
     */
    default void preRestart(Throwable reason) {}

    /**
     * Actor重启后的后处理钩子
     * - 在Actor重启完成后调用
     * - 用于恢复状态、重新初始化资源等
     * - 可以访问到重启前保存的状态
     *
     * @param reason 导致重启的原因(异常)
     */
    default void postRestart(Throwable reason) {}

    /**
     * Actor停止前的预处理钩子
     * - 在Actor即将停止前调用
     * - 用于清理资源、保存状态等收尾工作
     * - 停止后Actor将不再处理新消息
     */
    default void preStop() {}

    /**
     * Actor恢复前的预处理钩子
     * - 在Actor从暂停状态恢复前调用
     * - 用于重新建立连接、恢复缓存等
     */
    default void preResume() {}

    /**
     * Actor暂停前的预处理钩子
     * - 在Actor即将暂停前调用
     * - 用于保存临时状态、释放资源等
     * - 暂停后Actor将暂时不处理新消息
     */
    default void preSuspend() {}
    /**
     * 在Actor接收消息之前执行
     * @param message 消息
     * @param next 下一个处理者
     */
    default void aroundReceive(Envelope message, Consumer<Envelope> next) {
        next.accept(message);
    }
}
