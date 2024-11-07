package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;

public interface UserGuardianActorMessage {

    class CreateUserActor implements UserGuardianActorMessage {
        private final Class<? extends AbstractActor<?>> actorClass;
        private final String name;

        public CreateUserActor(Class<? extends AbstractActor<?>> actorClass, String name) {
            this.actorClass = actorClass;
            this.name = name;
        }

        public Class<? extends AbstractActor<?>> getActorClass() {
            return actorClass;
        }

        public String getName() {
            return name;
        }
    }

    class StopUserActor implements UserGuardianActorMessage {
        private final String name;

        public StopUserActor(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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