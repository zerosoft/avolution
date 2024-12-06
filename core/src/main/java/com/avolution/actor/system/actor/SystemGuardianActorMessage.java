package com.avolution.actor.system.actor;

import com.avolution.actor.core.UnTypedActor;
import com.avolution.actor.core.ActorRef;

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
}
