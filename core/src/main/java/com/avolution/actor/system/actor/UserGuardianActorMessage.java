package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;

public interface UserGuardianActorMessage {

    class CreateUserActor implements UserGuardianActorMessage {
        private final Class<? extends AbstractActor<?>> actorClass;
        private final String name;

        public CreateUserActor(Class<? extends AbstractActor<?>> actorClass, String name) {
            this.actorClass = actorClass;
            this.name = name;
        }

        public Class getActorClass() {
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

    // Actor 创建成功响应
    class ActorCreated implements UserGuardianActorMessage {
        private final ActorRef<?> actorRef;

        public ActorCreated(ActorRef<?> actorRef) {
            this.actorRef = actorRef;
        }

        public ActorRef<?> getActorRef() {
            return actorRef;
        }
    }

    // Actor 创建失败响应
    class ActorCreationFailed implements UserGuardianActorMessage {
        private final String name;
        private final Throwable cause;

        public ActorCreationFailed(String name, Throwable cause) {
            this.name = name;
            this.cause = cause;
        }

        public String getName() {
            return name;
        }

        public Throwable getCause() {
            return cause;
        }
    }

    // Actor 停止成功响应
    class ActorStopped implements UserGuardianActorMessage {
        private final String name;

        public ActorStopped(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Actor 停止失败响应
    class ActorStopFailed implements UserGuardianActorMessage {
        private final String name;
        private final Throwable cause;

        public ActorStopFailed(String name, Throwable cause) {
            this.name = name;
            this.cause = cause;
        }

        public String getName() {
            return name;
        }

        public Throwable getCause() {
            return cause;
        }
    }

    // Actor 重启成功响应
    class ActorRestarted implements UserGuardianActorMessage {
        private final String name;
        private final ActorRef<?> newRef;

        public ActorRestarted(String name, ActorRef<?> newRef) {
            this.name = name;
            this.newRef = newRef;
        }

        public String getName() {
            return name;
        }

        public ActorRef<?> getNewRef() {
            return newRef;
        }
    }

    // Actor 重启失败响应
    class ActorRestartFailed implements UserGuardianActorMessage {
        private final String name;
        private final Throwable cause;

        public ActorRestartFailed(String name, Throwable cause) {
            this.name = name;
            this.cause = cause;
        }

        public String getName() {
            return name;
        }

        public Throwable getCause() {
            return cause;
        }
    }
}