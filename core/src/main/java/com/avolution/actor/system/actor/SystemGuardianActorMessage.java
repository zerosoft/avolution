package com.avolution.actor.system.actor;

import com.avolution.actor.core.Props;
import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.ActorRef;

import java.util.concurrent.CompletableFuture;

public interface SystemGuardianActorMessage {
    class StartActorMessage implements SystemGuardianActorMessage {
        private final Class<? extends UnTypedActor<?>> actorClass;
        private final String name;

        public StartActorMessage(Class<? extends UnTypedActor<?>> actorClass, String name) {
            this.actorClass = actorClass;
            this.name = name;
        }

        public Class<? extends UnTypedActor<?>> getActorClass() {
            return actorClass;
        }

        public String getName() {
            return name;
        }
    }

    class StopActorMessage implements SystemGuardianActorMessage {
        private final ActorRef<?> actorRef;

        public StopActorMessage(ActorRef<?> actorRef) {
            this.actorRef = actorRef;
        }

        public ActorRef<?> getActorRef() {
            return actorRef;
        }
    }

    class RestartActorMessage implements SystemGuardianActorMessage {
        private final ActorRef<?> actorRef;

        public RestartActorMessage(ActorRef<?> actorRef) {
            this.actorRef = actorRef;
        }

        public ActorRef<?> getActorRef() {
            return actorRef;
        }
    }

    class CreateAskActor implements SystemGuardianActorMessage {
        public final Props props;
        public final String name;
        public final CompletableFuture<ActorRef> future;

        public CreateAskActor(Props props, String name, CompletableFuture<ActorRef> future) {
            this.props = props;
            this.name = name;
            this.future = future;
        }

    }
}
