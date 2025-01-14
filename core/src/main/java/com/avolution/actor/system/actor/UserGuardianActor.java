package com.avolution.actor.system.actor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.TypedActor;
import com.avolution.actor.exception.ActorCreationException;

/**
 * 用户守护Actor
 */
public class UserGuardianActor extends TypedActor<UserGuardianActorMessage> {

    private static final Logger logger = LoggerFactory.getLogger(UserGuardianActor.class);

    private final ActorSystem actorSystem;

    private final Map<String, ActorRef<?>> childActors = new ConcurrentHashMap<>();

    public UserGuardianActor(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public boolean preStart() {
        logger.debug("UserGuardianActor started at path: {}", getContext().getPath());
        return super.preStart();

    }

    // 1. Actor 创建流程
    private void handleCreateUserActor(UserGuardianActorMessage.CreateUserActor message) {
        try {
            String actorName = message.name;
            Props props = message.props;

            logger.debug("Creating actor '{}' under path '{}'", actorName, getContext().getPath());

            // 使用当前上下文创建Actor
            ActorRef actorRef = actorSystem.actorOf(props, actorName, getContext());

            // 监视新创建的Actor
            getContext().watch(actorRef);

            // 将新创建的Actor添加到子Actor列表中
            childActors.put(actorName, actorRef);

            // 等待Actor完全初始化
            waitForActorInitialization(actorRef);

            // 完成Future
            message.future.complete(actorRef);
            
            logger.debug("Successfully created actor: {} under {}", actorName, getContext().getPath());
        } catch (Exception e) {
            logger.error("Failed to create actor: {} under {}", message.name, getContext().getPath(), e);
            message.future.completeExceptionally(e);
        }
    }

    private void waitForActorInitialization(ActorRef<?> ref) throws InterruptedException {
        // 等待最多500ms让Actor完全初始化
        long deadline = System.currentTimeMillis() + 500;
        while (System.currentTimeMillis() < deadline) {
            if (getContext().getChildren().containsKey(ref.name())) {
                return;
            }
            Thread.sleep(10);
        }
        throw new ActorCreationException("Actor initialization timeout: " + ref.path());
    }

    // 2. Actor 停止流程
    private void handleStopUserActor(UserGuardianActorMessage.StopUserActor message) {
        String actorName = message.name;
        ActorRef<?> child = childActors.remove(actorName);
        if (child != null) {
            logger.debug("Stopping actor: {}", child.path());
            getContext().stop(child);
        }
    }


    @Override
    protected void onReceive(UserGuardianActorMessage message) throws Exception {
        // 处理消息
        switch (message) {
            case UserGuardianActorMessage.CreateUserActor createUserActor -> 
                handleCreateUserActor(createUserActor);
            case UserGuardianActorMessage.StopUserActor stopUserActor -> 
                handleStopUserActor(stopUserActor);
            case UserGuardianActorMessage.RestartUserActor restartUserActor -> {
                // 暂不处理重启
            }
            default -> throw new IllegalArgumentException("Unknown message type: " + message);
        }
    }
}