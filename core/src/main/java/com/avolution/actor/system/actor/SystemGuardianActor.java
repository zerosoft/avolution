package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.Props;
import com.avolution.actor.core.annotation.OnReceive;


public class SystemGuardianActor extends AbstractActor<SystemGuardianActorMessage> {

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
        // 停止指定的Actor
//        context.stopSelf(actorRef);
        System.out.println("Stopped actor: " + actorRef.path());
    }

    private void restartActor(ActorRef<?> actorRef) {
        // 重启指定的Actor
//        context.stopSelf(actorRef);
        // 这里可以添加逻辑来重新创建Actor
        System.out.println("Restarted actor: " + actorRef.path());
    }


}
