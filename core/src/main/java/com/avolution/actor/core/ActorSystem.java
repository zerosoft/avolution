package com.avolution.actor.core;

import com.avolution.actor.config.ActorSystemConfig;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.extension.Extension;
import com.avolution.actor.extension.ExtensionId;
import com.avolution.actor.supervision.DeathWatch;
import com.avolution.actor.routing.RouterManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Actor系统接口，定义了Actor系统的核心功能
 * 
 * <p>ActorSystem是整个Actor框架的核心，负责：
 * <ul>
 *   <li>Actor的创建和管理</li>
 *   <li>消息的调度和分发</li>
 *   <li>系统资源的管理</li>
 *   <li>生命周期的控制</li>
 * </ul>
 */
public interface ActorSystem {

    /**
     * 获取Actor系统名称
     *
     * @return Actor系统的名称
     */
    String name();

    /**
     * 创建新的Actor实例
     *
     * @param props Actor的配置属性
     * @param name Actor的名称
     * @return 新创建的ActorRef
     * @throws IllegalArgumentException 如果name为null或已存在
     * @throws ActorCreationException 如果Actor创建失败
     */
    ActorRef actorOf(Props props, String name);

    /**
     * 根据路径查找Actor
     *
     * @param path Actor的完整路径
     * @return 包含ActorRef的Optional，如果未找到则为empty
     */
    Optional<ActorRef> findActor(String path);

    /**
     * 获取指定路径下的所有子Actor
     *
     * @param parentPath 父Actor的路径
     * @return 子Actor列表
     */
    List<ActorRef> getChildActors(String parentPath);

    /**
     * 创建临时Actor
     * 临时Actor会在完成任务后自动销毁
     *
     * @param props Actor的配置属性
     * @return 临时Actor的引用
     */
    ActorRef createTempActor(Props props);

    /**
     * 获取消息调度器
     *
     * @return 系统使用的Dispatcher实例
     */
    Dispatcher dispatcher();

    /**
     * 获取死亡监视器
     *
     * @return DeathWatch实例
     */
    DeathWatch deathWatch();

    /**
     * 获取路由管理器
     *
     * @return RouterManager实例
     */
    RouterManager router();

    /**
     * 获取调度器服务
     *
     * @return ScheduledExecutorService实例
     */
    ScheduledExecutorService scheduler();

    /**
     * 获取死信Actor引用
     * 用于处理无法投递的消息
     *
     * @return 死信Actor的引用
     */
    ActorRef deadLetters();

    /**
     * 获取或创建系统扩展
     *
     * @param extensionId 扩展ID
     * @return 扩展实例
     * @param <T> 扩展类型
     */
    <T extends Extension> T extension(ExtensionId<T> extensionId);

    /**
     * 终止Actor系统
     * 会优雅地关闭所有Actor和系统资源
     *
     * @return 完成阶段，当系统完全终止时完成
     */
    CompletionStage<Void> terminate();

    /**
     * 等待系统终止
     *
     * @param timeout 最大等待时间
     * @throws InterruptedException 如果等待被中断
     */
    void awaitTermination(Duration timeout) throws InterruptedException;

    /**
     * 获取系统配置
     *
     * @return 系统配置实例
     */
    ActorSystemConfig getConfig();

    /**
     * 获取系统当前状态
     *
     * @return 系统状态
     */
    SystemState getState();

    /**
     * 检查系统是否正在运行
     *
     * @return 如果系统正在运行返回true
     */
    boolean isRunning();

    /**
     * 获取系统状态信息
     *
     * @return 系统状态信息
     */
    SystemStatus getSystemStatus();

    /**
     * 获取Actor统计信息
     *
     * @return Actor统计信息
     */
    ActorStats getActorStats();

    /**
     * 系统状态枚举
     */
    enum SystemState {
        INITIALIZING,
        RUNNING,
        TERMINATING,
        TERMINATED
    }

    /**
     * 系统状态信息类
     */
    class SystemStatus {
        private final SystemState state;
        private final int actorCount;
        private final int activeDispatcherCount;
        private final int activeSchedulerCount;

        public SystemStatus(SystemState state, int actorCount, 
                          int activeDispatcherCount, int activeSchedulerCount) {
            this.state = state;
            this.actorCount = actorCount;
            this.activeDispatcherCount = activeDispatcherCount;
            this.activeSchedulerCount = activeSchedulerCount;
        }

        public SystemState getState() { return state; }
        public int getActorCount() { return actorCount; }
        public int getActiveDispatcherCount() { return activeDispatcherCount; }
        public int getActiveSchedulerCount() { return activeSchedulerCount; }
    }

    /**
     * Actor统计信息类
     */
    class ActorStats {
        private final int totalActors;
        private final Map<String, Integer> actorTypeCount;
        private final long messageCount;
        private final long errorCount;

        public ActorStats(int totalActors, Map<String, Integer> actorTypeCount,
                         long messageCount, long errorCount) {
            this.totalActors = totalActors;
            this.actorTypeCount = actorTypeCount;
            this.messageCount = messageCount;
            this.errorCount = errorCount;
        }

        public int getTotalActors() { return totalActors; }
        public Map<String, Integer> getActorTypeCount() { return actorTypeCount; }
        public long getMessageCount() { return messageCount; }
        public long getErrorCount() { return errorCount; }
    }

    /**
     * Actor创建异常
     */
    class ActorCreationException extends RuntimeException {
        public ActorCreationException(String message) {
            super(message);
        }

        public ActorCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
