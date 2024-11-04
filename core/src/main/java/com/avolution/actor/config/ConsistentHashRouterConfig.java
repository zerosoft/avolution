package com.avolution.actor.config;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.router.RouterConfig;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 一致性哈希路由
 */
public class ConsistentHashRouterConfig extends RouterConfig {
    private final Function<Object, Object> hashMapping;
    private final int virtualNodesFactor;

    private ConsistentHashRouterConfig(Builder builder) {
        super(builder);
        this.hashMapping = builder.hashMapping;
        this.virtualNodesFactor = builder.virtualNodesFactor;
    }

    @Override
    public ActorRef selectRoutee(List<ActorRef> routees, Envelope message) {
        if (routees.isEmpty()) {
            throw new IllegalStateException("No routees available");
        }
        Object key = hashMapping.apply(message.message());
        int hash = key.hashCode();
        // 实现一致性哈希选择逻辑
        return routees.get(Math.abs(hash % routees.size()));
    }

    @Override
    public String getRoutingLogic() {
        return "consistent-hash";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends RouterConfig.Builder<Builder> {
        private Function<Object, Object> hashMapping = Object::hashCode;
        private int virtualNodesFactor = 10;

        public Builder hashMapping(Function<Object, Object> mapping) {
            this.hashMapping = mapping;
            return this;
        }

        public Builder virtualNodesFactor(int factor) {
            this.virtualNodesFactor = factor;
            return this;
        }

        @Override
        public ConsistentHashRouterConfig build() {
            validate();
            Objects.requireNonNull(hashMapping, "Hash mapping cannot be null");
            if (virtualNodesFactor <= 0) {
                throw new IllegalArgumentException("Virtual nodes factor must be positive");
            }
            return new ConsistentHashRouterConfig(this);
        }
    }
}