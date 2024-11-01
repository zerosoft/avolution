package com.avolution.actor.extension;


import com.avolution.actor.core.ActorSystem;

/**
 * Actor系统扩展基类
 */
public abstract class Extension {
    protected final ActorSystem system;

    protected Extension(ActorSystem system) {
        this.system = system;
    }

    /**
     * 扩展初始化
     */
    public abstract void initialize();

    /**
     * 扩展关闭
     */
    public abstract void shutdown();

    /**
     * 获取扩展名称
     */
    public abstract String name();

    /**
     * 获取扩展版本
     */
    public abstract String version();

    /**
     * 检查扩展是否已初始化
     */
    public abstract boolean isInitialized();

    /**
     * 获取扩展描述
     */
    public abstract String description();

    /**
     * 获取扩展配置
     */
    public abstract ExtensionConfig getConfig();

    /**
     * 获取Actor系统
     */
    public ActorSystem system() {
        return system;
    }
} 