package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.message.Signal;
import com.avolution.actor.util.ActorPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 系统启动和初始化
 * Actor系统初始化：SystemGuardianActor是Actor系统启动时第一个被创建的Actor，它负责启动系统级别的Actor和服务。
 * 监视系统Actor：它监视系统中其他关键Actor的生命周期，确保它们正常启动。
 * 2. 生命周期管理
 * 启动和停止：SystemGuardianActor控制整个Actor系统的启动和停止过程。
 * 生命周期事件：它接收并处理系统级别的生命周期事件，如系统启动完成、系统停止等。
 * 3. 错误处理和监督
 * 全局监督策略：SystemGuardianActor定义了系统级别的监督策略，处理系统Actor的失败情况。
 * 错误传播：当系统Actor发生错误时，SystemGuardianActor负责将错误信息传播给其他相关Actor或监督者。
 * 重启和停止：根据监督策略，决定是否重启或停止失败的Actor。
 * 4. 系统资源管理
 * 资源分配：它可能参与系统资源的分配，如线程池、调度器等的配置和管理。
 * 资源清理：在系统停止时，确保所有系统资源被正确释放。
 * 5. 系统级别事件处理
 * 系统事件：处理系统级别的消息和事件，如Actor系统配置变化、系统日志、系统监控等。
 * 事件发布：发布系统事件，以便其他部分可以订阅和响应这些事件。
 * 6. 安全和访问控制
 * 权限管理：在某些Akka实现中，SystemGuardianActor可能参与系统级别的权限管理，控制对Actor系统的访问。
 * 安全策略：它可能执行系统级别的安全策略，确保系统的安全性。
 * 7. 系统状态监控
 * 健康检查：监控Actor系统的整体健康状态，包括内存使用、线程池状态等。
 * 诊断信息：提供系统级别的诊断信息，帮助排查问题。
 * 8. 系统配置
 * 配置加载：参与系统配置的加载和应用。
 * 配置变更：处理系统配置的动态变更。
 * 9. 系统终止
 * 系统关闭：当系统需要关闭时，SystemGuardianActor负责执行系统级别的关闭操作，确保所有Actor和资源被正确停止和清理。
 */
public class SystemGuardianActor extends AbstractActor<SystemGuardianActorMessage> {

    private Logger logger= LoggerFactory.getLogger(SystemGuardianActor.class);

    @OnReceive(SystemGuardianActorMessage.StartActorMessage.class)
    private void handleStartActor(SystemGuardianActorMessage.StartActorMessage message) {
        startActor(message.getActorClass(), message.getName());
    }

    @OnReceive(SystemGuardianActorMessage.StopActorMessage.class)
    private void handleStopActor(SystemGuardianActorMessage.StopActorMessage message) {
        stopActor(message.getActorRef());
    }

    @OnReceive(SystemGuardianActorMessage.RestartActorMessage.class)
    private void handleRestartActor(SystemGuardianActorMessage.RestartActorMessage message) {
        restartActor(message.getActorRef());
    }

    private void startActor(Class actorClass, String name) {
        // 创建并启动新的Actor
        ActorRef actorRef = context.getActorSystem().actorOf(Props.create(actorClass), name);
        System.out.println("Started actor: " + name);
    }

    private void stopActor(ActorRef<?> actorRef) {
        // 获取Actor的监视者
        Set<String> watchers = context.getActorSystem().getRefRegistry().getWatchers(actorRef.path());

        // 停止Actor
        actorRef.tell(Signal.STOP, getSelfRef());

        // 通知所有监视者
        watchers.forEach(watcherPath -> {
            ActorRef<?> watcher = context.getActorSystem().getRefRegistry().getRef(watcherPath);
            if (watcher != null) {
                watcher.tell(Signal.TERMINATED, getSelfRef());
            }
        });

        logger.info("Stopped actor: {}", actorRef.path());
    }

    private void restartActor(ActorRef<?> actorRef) {

    }


}
