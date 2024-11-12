package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.annotation.OnReceive;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户守护Actor
 */
public class UserGuardianActor extends AbstractActor<UserGuardianActorMessage> {
    private final Map<String, ActorRef<?>> childActors = new HashMap<>();

    @OnReceive(UserGuardianActorMessage.CreateUserActor.class)
    private void handleCreateUserActor(UserGuardianActorMessage.CreateUserActor message) {
        String actorName = message.getName();
        Class actorClass = message.getActorClass();

        // 创建并启动用户Actor
        ActorRef<?> actorRef = context.system().actorOf(Props.create(actorClass), actorName);
        childActors.put(actorName, actorRef);
        System.out.println("Created user actor: " + actorName);
    }

    @OnReceive(UserGuardianActorMessage.StopUserActor.class)
    private void handleStopUserActor(UserGuardianActorMessage.StopUserActor message) {
        String actorName = message.getName();
        ActorRef<?> actorRef = childActors.remove(actorName);

        if (actorRef != null) {
            context.stop(actorRef);
            System.out.println("Stopped user actor: " + actorName);
        } else {
            System.out.println("User actor not found: " + actorName);
        }
    }

    @OnReceive(UserGuardianActorMessage.RestartUserActor.class)
    private void handleRestartUserActor(UserGuardianActorMessage.RestartUserActor message) {
        String actorName = message.getName();
        ActorRef<?> actorRef = childActors.get(actorName);

        if (actorRef != null) {
            context.stop(actorRef);
            // 重新创建Actor
            Class actorClass = message.getActorClass();
            ActorRef<?> newActorRef = context.system().actorOf(Props.create(actorClass), actorName);
            childActors.put(actorName, newActorRef);
            System.out.println("Restarted user actor: " + actorName);
        } else {
            System.out.println("User actor not found for restart: " + actorName);
        }
    }



    @Override
    public void preStart() {
        System.out.println("UserGuardianActor starting.");
    }

    @Override
    public void onPostStop() {
        System.out.println("UserGuardianActor stopped.");
    }

    @Override
    public void onPreRestart(Throwable reason) {
        System.out.println("UserGuardianActor is restarting due to: " + reason.getMessage());
    }

    @Override
    public void onPostRestart(Throwable reason) {
        System.out.println("UserGuardianActor has restarted.");
    }
}
