package com.avolution.actor.config;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.router.RouterConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询路由
 */
public class RoundRobinRouterConfig extends RouterConfig {
    private final AtomicInteger counter = new AtomicInteger(0);

    private RoundRobinRouterConfig(Builder builder) {
        super(builder);
    }

    @Override
    public ActorRef selectRoutee(List<ActorRef> routees, Envelope message) {
        if (routees.isEmpty()) {
            throw new IllegalStateException("No routees available");
        }
        int index = Math.abs(counter.getAndIncrement() % routees.size());
        return routees.get(index);
    }

    @Override
    public String getRoutingLogic() {
        return "round-robin";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends RouterConfig.Builder<Builder> {
        @Override
        public RoundRobinRouterConfig build() {
            validate();
            return new RoundRobinRouterConfig(this);
        }
    }
}



