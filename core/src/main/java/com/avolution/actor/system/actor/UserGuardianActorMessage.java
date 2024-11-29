package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.Props;

import java.util.concurrent.CompletableFuture;

public interface UserGuardianActorMessage {

    class CreateUserActor implements UserGuardianActorMessage {
        public final Props props;
        public final String name;
        public final CompletableFuture<ActorRef> future;

        public CreateUserActor(Props props, String name, CompletableFuture<ActorRef> future) {
            this.props = props;
            this.name = name;
            this.future = future;
        }

    }

    class StopUserActor implements UserGuardianActorMessage {
        public final String name;

        public StopUserActor(String actorPath) {
            this.name = actorPath;
        }

    }

    class RestartUserActor implements UserGuardianActorMessage {
        private final String name;
        private final Class<? extends AbstractActor<?>> actorClass;

        public RestartUserActor(String name, Class<? extends AbstractActor<?>> actorClass) {
            this.name = name;
            this.actorClass = actorClass;
        }

        public String getName() {
            return name;
        }

        public Class<? extends AbstractActor<?>> getActorClass() {
            return actorClass;
        }
    }




}