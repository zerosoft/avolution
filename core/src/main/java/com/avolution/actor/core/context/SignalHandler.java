package com.avolution.actor.core.context;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Priority;
import com.avolution.actor.message.Signal;
import com.avolution.actor.message.SignalScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 *   信号处理器
 */
public class SignalHandler {

    private final ActorContext context;
    private final Logger logger = LoggerFactory.getLogger(SignalHandler.class);

    /**
     * 信号处理器构造函数
     * @param context Actor上下文
     */
    public SignalHandler(ActorContext context) {
        this.context = context;
    }

    /**
     * 处理接收到的信号
     * 根据不同的信号类型调用相应的处理方法
     * @param envelope 信号封装对象
     */
    public void handle(Envelope envelope) {
        Signal signal = (Signal) envelope.getMessage();
        switch (signal) {
            // 处理各类信号
            case STOP -> handleStopSignal();              // 优雅停止信号
            case KILL -> handleKillSignal();              // 强制终止信号
            case RESTART -> handleRestartSignal(          // 重启信号
                    (Throwable) envelope.getMetadata("cause")
            );
            case SUSPEND -> handleSuspendSignal();        // 暂停信号
            case RESUME -> handleResumeSignal();          // 恢复信号
            case ESCALATE -> handleEscalateSignal(        // 错误升级信号
                    (Throwable) envelope.getMetadata("cause"),
                    (ActorRef<?>) envelope.getMetadata("child"),
                    (Envelope) envelope.getMetadata("failedMessage")
            );
            case POISON_PILL -> handlePoisonPill((CompletableFuture<Void>)envelope.getMetadata("stopFuture"));       // 毒丸信号
            case CHILD_TERMINATED -> handleChildTerminated(// 子Actor终止信号
                    (ActorRef<?>) envelope.getMetadata("child")
            );
            case SYSTEM_FAILURE -> handleSystemFailure(    // 系统故障信号
                    (Throwable) envelope.getMetadata("cause"),
                    (Envelope) envelope.getMetadata("failedMessage")
            );
            default -> logger.warn("收到未知信号: {}", signal);
        }
    }

    /**
     * 处理强制终止信号
     * 立即终止Actor，不等待消息处理完成
     */
    private void handleKillSignal() {
        context.stop();      // 清理资源
    }

    /**
     * 处理优雅停止信号
     * 等待当前消息处理完成后再终止
     */
    private void handleStopSignal() {
        context.stop(new CompletableFuture<>());
    }

    /**
     * 处理毒丸信号
     * 处理完当前邮箱中的所有消息后终止
     */
    private void handlePoisonPill(CompletableFuture<Void> stopFuture) {
        context.stop(stopFuture);
    }

    /**
     * 处理子Actor终止信号
     * @param child 已终止的子Actor引用
     */
    private void handleChildTerminated(ActorRef<?> child) {
        context.removeChild(child);
        context.unwatch(child);

        // 如果Actor正在停止且没有存活的子Actor，则完成终止
//        if (lifecycle.isStopping() && context.getChildren().isEmpty()) {
//            lifecycle.terminate();
//            context.cleanup();
//        }
    }

    /**
     * 处理系统故障信号
     * @param cause 故障原因
     * @param failedMessage 导致故障的消息
     */
    private void handleSystemFailure(Throwable cause, Envelope failedMessage) {
        logger.error("Actor系统故障: {}", context.getUnTypedActor().path(), cause);
        context.getActorSystem().handleSystemFailure(cause, context.getUnTypedActor());
//        lifecycle.terminate();
//        context.cleanup();
    }

    /**
     * 处理重启信号
     * @param cause 导致重启的原因
     */
    private void handleRestartSignal(Throwable cause) {
        try {
//            context.getSelf().preRestart(cause);    // 重启前回调

            // 停止所有子Actor
            context.getChildren().values().forEach(child ->
                    context.getActorSystem().stop(child)
            );

//            lifecycle.restart();            // 重启生命周期
//            context.getSelf().postRestart(cause);   // 重启后回调

        } catch (Exception e) {
            logger.error("Actor重启失败: {}", context.getUnTypedActor().path(), e);
            handleSystemFailure(e, null);
        }
    }

    /**
     * 处理暂停信号
     * 暂停消息处理
     */
    private void handleSuspendSignal() {
//        lifecycle.suspend();
        context.suspend();
    }

    /**
     * 处理恢复信号
     * 恢复消息处理
     */
    private void handleResumeSignal() {
        context.resume();
//        lifecycle.resume();
    }

    /**
     * 处理错误升级信号
     * 将错误传递给父Actor处理
     * @param cause 错误原因
     * @param child 发生错误的子Actor
     * @param failedMessage 导致错误的消息
     */
    private void handleEscalateSignal(Throwable cause, ActorRef<?> child, Envelope failedMessage) {
        ActorContext parent = context.getParent();
        if (parent == null) {
            handleSystemFailure(cause, failedMessage);
            return;
        }

        Envelope escalateSignal = Envelope.builder()
                .message(Signal.ESCALATE)
                .sender(context.getUnTypedActor())
                .recipient(parent.getUnTypedActor().getSelfRef())
                .priority(Priority.HIGH)
                .scope(SignalScope.SINGLE)
                .build();

        escalateSignal.addMetadata("cause", cause);
        escalateSignal.addMetadata("child", child);
        escalateSignal.addMetadata("failedMessage", failedMessage);

        parent.getUnTypedActor().tell(escalateSignal);
    }
}