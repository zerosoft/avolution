package com.avolution.actor.extension;


import com.avolution.actor.core.ActorSystem;

/**
 * 扩展ID类
 */
public abstract class ExtensionId<T extends Extension> {
    private final String id;
    private final ExtensionConfig config;

    protected ExtensionId(String id) {
        this(id, ExtensionConfig.defaultConfig());
    }

    protected ExtensionId(String id, ExtensionConfig config) {
        this.id = id;
        this.config = config;
    }

    /**
     * 创建扩展实例
     */
    public abstract T createExtension(ActorSystem system);

    /**
     * 获取扩展ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取扩展配置
     */
    public ExtensionConfig getConfig() {
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtensionId)) return false;
        ExtensionId<?> that = (ExtensionId<?>) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

/**
 * 扩展配置类
 */
class ExtensionConfig {
    private final boolean autoStart;
    private final int priority;
    private final boolean required;
    private final String version;

    private ExtensionConfig(Builder builder) {
        this.autoStart = builder.autoStart;
        this.priority = builder.priority;
        this.required = builder.required;
        this.version = builder.version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExtensionConfig defaultConfig() {
        return builder().build();
    }

    public boolean isAutoStart() { return autoStart; }
    public int getPriority() { return priority; }
    public boolean isRequired() { return required; }
    public String getVersion() { return version; }

    public static class Builder {
        private boolean autoStart = true;
        private int priority = 0;
        private boolean required = false;
        private String version = "1.0.0";

        public Builder autoStart(boolean autoStart) {
            this.autoStart = autoStart;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public ExtensionConfig build() {
            return new ExtensionConfig(this);
        }
    }
} 