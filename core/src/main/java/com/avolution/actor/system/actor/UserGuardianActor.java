package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.annotation.OnReceive;
import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.exception.ActorCreationException;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Signal;
import com.avolution.actor.message.SignalEnvelope;
import com.avolution.actor.message.SignalScope;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;
import com.avolution.actor.supervision.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 用户守护Actor
 */
public class UserGuardianActor extends AbstractActor<UserGuardianActorMessage> {

    private static final Logger logger = LoggerFactory.getLogger(UserGuardianActor.class);

    private final Map<String, ActorRef<?>> childActors = new ConcurrentHashMap<>();

    private final SupervisorStrategy supervisorStrategy;

    public UserGuardianActor() {
        this.supervisorStrategy = new DefaultSupervisorStrategy();
    }

    // 1. Actor 创建流程
    @OnReceive(UserGuardianActorMessage.CreateUserActor.class)
    private void handleCreateUserActor(UserGuardianActorMessage.CreateUserActor message) {
        String actorName = message.getName();
        try {
            // 1.1 创建 Actor
            Props props = Props.create(message.getActorClass());

            // 1.2 实例化并启动 Actor
            ActorRef<?> actorRef = context.actorOf(props, actorName);

            // 1.3 添加到子 Actor 列表
            childActors.put(actorName, actorRef);

            // 1.4 监控生命周期
            context.watch(actorRef);

            // 1.5 通知创建成功
            getSender().tell(new UserGuardianActorMessage.ActorCreated(actorRef), getSelfRef());

        } catch (Exception e) {
            logger.error("Failed to create actor: {}", actorName, e);
            getSender().tell(new UserGuardianActorMessage.ActorCreationFailed(actorName, e), getSelfRef());
        }
    }

    // 2. Actor 停止流程
    @OnReceive(UserGuardianActorMessage.StopUserActor.class)
    private void handleStopUserActor(UserGuardianActorMessage.StopUserActor message) {
        String actorName = message.getName();
        ActorRef<?> actorRef = childActors.get(actorName);
        if (actorRef != null) {
            // 2.1 停止 Actor
            stopChild(actorRef)
                    .thenRun(() -> {
                        // 2.2 从子 Actor 列表移除
                        childActors.remove(actorName);
                        // 2.3 通知停止成功
                        getSender().tell(new UserGuardianActorMessage.ActorStopped(actorName), getSelfRef());
                    })
                    .exceptionally(e -> {
                        logger.error("Failed to stop actor: {}", actorName, e);
                        getSender().tell(new UserGuardianActorMessage.ActorStopFailed(actorName, e), getSelfRef());
                        return null;
                    });
        }
    }

    private CompletableFuture<Void> stopChild(ActorRef<?> child) {
        try {
            // 1. 获取子Actor的上下文
            ActorContext childContext = context.getActorSystem()
                    .getContextManager()
                    .getContext(child.path())
                    .orElseThrow(() -> new IllegalStateException("Actor context not found: " + child.path()));

            // 2. 发送停止信号
            child.tell(Signal.STOP, getSelfRef());

            // 3. 监控停止过程
            CompletableFuture<Void> stopFuture = new CompletableFuture<>();
            childContext.watch(child, () -> {
                context.getActorSystem().unregisterActor(child.path());
                stopFuture.complete(null);
            });

            return stopFuture;
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // 3. 监督和重启流程
//    @OnReceive(Signal.SUPERVISION_DIRECTIVE)
//    private void handleSupervision(Signal signal) {
//        ActorRef<?> child = signal.getSource();
//        Throwable failure = (Throwable) signal.getMetadata().get("failure");
//
//        // 3.1 根据监督策略决定处理方式
//        switch (supervisorStrategy.handle(failure)) {
//            case RESTART -> restartChild(child);
//            case STOP -> stopChild(child);
//            case ESCALATE -> escalateFailure(child, failure);
//        }
//    }
//
//    // 4. 子 Actor 终止处理
//    @OnReceive(Signal.CHILD_TERMINATED)
//    private void handleChildTerminated(Signal signal) {
//        ActorRef<?> child = signal.getSource();
//        // 4.1 清理子 Actor 引用
//        childActors.remove(child.name());
//        // 4.2 发布终止事件
//        context.getActorSystem().getEventStream().publish(
//                new ActorLifecycleEvent(child.path(), ActorLifecycleEvent.Type.TERMINATED)
//        );
//    }
//
//    @Override
//    public void preStart() {
//        logger.info("UserGuardian starting at path: {}", context.getPath());
//        // 订阅生命周期事件
//        context.getActorSystem().getEventStream().subscribe(
//                ActorLifecycleEvent.class,
//                getSelf()
//        );
//    }
//
//    @Override
//    public void postStop() {
//        logger.info("UserGuardian stopping, terminating all children");
//        // 停止所有子 Actor
//        terminateChildren();
//        // 取消事件订阅
//        context.getActorSystem().getEventStream().unsubscribe(
//                ActorLifecycleEvent.class,
//                getSelf()
//        );
//    }
}