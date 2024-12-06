package com.avolution.actor.system.actor;

import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.annotation.OnReceive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户守护Actor
 */
public class UserGuardianActor extends UnTypedActor<UserGuardianActorMessage> {

    private static final Logger logger = LoggerFactory.getLogger(UserGuardianActor.class);

    private final ActorSystem actorSystem;

    private final Map<String, ActorRef<?>> childActors = new ConcurrentHashMap<>();

    public UserGuardianActor(ActorSystem actorSystem) {
        this.actorSystem=actorSystem;
    }

    // 1. Actor 创建流程
    @OnReceive(UserGuardianActorMessage.CreateUserActor.class)
    private void handleCreateUserActor(UserGuardianActorMessage.CreateUserActor message) {
        try {
            String actorName = message.name;

            Props props = message.props;

            ActorRef actorRef = actorSystem.actorOf(props, actorName, getContext());

            context.watch(actorRef);

            childActors.put(actorName, actorRef);

            message.future.complete(actorRef);

        } catch (Exception e) {
            logger.error("Failed to create actor: {}",  message.name, e);
        }
    }

    // 2. Actor 停止流程
    @OnReceive(UserGuardianActorMessage.StopUserActor.class)
    private void handleStopUserActor(UserGuardianActorMessage.StopUserActor message) {
        String actorName = message.name;
        childActors.remove(actorName);
    }


}