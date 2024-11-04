package com.avolution.actor.core;

import com.avolution.actor.context.ActorContext;
import com.avolution.actor.dispatch.Dispatcher;
import com.avolution.actor.router.RouterConfig;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.supervision.DefaultSupervisorStrategy;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Actor属性配置类，用于创建Actor实例
 * @param <T> Actor处理的消息类型
 */
public class Props<T> {
    private final Class<? extends AbstractActor<T>> actorClass;
    private final Object[] args;
    private final SupervisorStrategy supervisorStrategy;
    private final Optional<RouterConfig> routerConfig;
    private final Optional<Function<ActorContext<T>, AbstractActor<T>>> creator;
    private final Optional<Dispatcher> dispatcher;

    private Props(Builder<T> builder) {
        this.actorClass = builder.actorClass;
        this.args = builder.args;
        this.supervisorStrategy = builder.supervisorStrategy;
        this.routerConfig = Optional.ofNullable(builder.routerConfig);
        this.creator = Optional.ofNullable(builder.creator);
        this.dispatcher = Optional.ofNullable(builder.dispatcher);
    }

    public static <T> Builder<T> builder(Class<? extends AbstractActor<T>> actorClass) {
        return new Builder<T>().withActorClass(actorClass);
    }

    public static <T> Props<T> create(Class<? extends AbstractActor<T>> actorClass, Object... args) {
        return builder(actorClass).withArgs(args).build();
    }

    public static <T> Props<T> create(Function<ActorContext<T>, AbstractActor<T>> creator) {
        return new Builder<T>().withCreator(creator).build();
    }

    public static class Builder<T> {
        private Class<? extends AbstractActor<T>> actorClass;
        private Object[] args = new Object[0];
        private SupervisorStrategy supervisorStrategy = new DefaultSupervisorStrategy();
        private RouterConfig routerConfig;
        private Function<ActorContext<T>, AbstractActor<T>> creator;
        private Dispatcher dispatcher;

        public Builder<T> withActorClass(Class<? extends AbstractActor<T>> actorClass) {
            this.actorClass = actorClass;
            return this;
        }

        public Builder<T> withArgs(Object... args) {
            this.args = args;
            return this;
        }


        public Builder<T> withSupervisorStrategy(SupervisorStrategy strategy) {
            this.supervisorStrategy = strategy;
            return this;
        }

        public Builder<T> withRouter(RouterConfig routerConfig) {
            this.routerConfig = routerConfig;
            return this;
        }

        public Builder<T> withCreator(Function<ActorContext<T>, AbstractActor<T>> creator) {
            this.creator = creator;
            return this;
        }

        public Builder<T> withDispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

        public Props<T> build() {
            validate();
            return new Props<>(this);
        }

        private void validate() {
            if (creator == null && actorClass == null) {
                throw new IllegalArgumentException("Either actorClass or creator must be specified");
            }
            if (creator != null && actorClass != null) {
                throw new IllegalArgumentException("Cannot specify both actorClass and creator");
            }
            Objects.requireNonNull(supervisorStrategy, "supervisorStrategy cannot be null");
        }
    }

    public Class<? extends AbstractActor<T>> getActorClass() {
        return actorClass;
    }

    public Object[] getArgs() {
        return args;
    }

    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    public Optional<RouterConfig> getRouterConfig() {
        return routerConfig;
    }

    public Optional<Function<ActorContext<T>, AbstractActor<T>>> getCreator() {
        return creator;
    }

    public Optional<Dispatcher> getDispatcher() {
        return dispatcher;
    }
}