package com.avolution.actor.config;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.router.RouterConfig;

import java.util.List;
import java.util.Random;

/**
 * 随机路由
 */
public class RandomRouterConfig extends RouterConfig {
    private final Random random = new Random();

    private RandomRouterConfig(Builder builder) {
        super(builder);
    }

    @Override
    public ActorRef selectRoutee(List<ActorRef> routees, Envelope message) {
        if (routees.isEmpty()) {
            throw new IllegalStateException("No routees available");
        }
        return routees.get(random.nextInt(routees.size()));
    }

    @Override
    public String getRoutingLogic() {
        return "random";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends RouterConfig.Builder<Builder> {
        @Override
        public RandomRouterConfig build() {
            validate();
            return new RandomRouterConfig(this);
        }
    }
}