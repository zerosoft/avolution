package com.avolution.actor.message;


/**
 * Actor系统信号枚举
 * 定义了所有Actor间通信的系统级信号
 */
public enum Signal {
    // ====== 生命周期信号 ======
    /**
     * 启动信号：用于初始化和启动Actor
     */
    START(SignalType.LIFECYCLE),
    
    /**
     * 停止信号：用于优雅地停止Actor，允许完成当前任务
     */
    STOP(SignalType.LIFECYCLE),
    
    /**
     * 重启信号：用于重新初始化Actor状态
     */
    RESTART(SignalType.LIFECYCLE),

    // ====== 系统控制信号 ======
    /**
     * 终止信号：表示Actor已经完全停止
     */
    TERMINATED(SignalType.LIFECYCLE),
    
    /**
     * 暂停信号：暂时停止处理新消息
     */
    SUSPEND(SignalType.CONTROL),
    
    /**
     * 恢复信号：恢复处理消息
     */
    RESUME(SignalType.CONTROL),
    
    /**
     * 毒丸信号：请求Actor在处理完当前消息后停止
     */
    POISON_PILL(SignalType.CONTROL),
    /**
     * 子Actor终止信号：子Actor已经停止
     */
    CHILD_TERMINATED(SignalType.SUPERVISION),
    
    /**
     * 强制终止信号：立即停止Actor，不等待消息处理完成
     */
    KILL(SignalType.CONTROL),
    
    /**
     * 系统故障信号：表示发生了严重的系统级错误
     */
    SYSTEM_FAILURE(SignalType.CONTROL),

    // ====== 监督信号 ======
    /**
     * 错误升级信号：将错误传递给父Actor处理
     */
    ESCALATE(SignalType.SUPERVISION),
    
    /**
     * 监督处理信号：触发监督策略处理
     */
    SUPERVISE(SignalType.SUPERVISION),

    // ====== 查询信号 ======
    /**
     * 状态查询信号：获取Actor当前状态信息
     */
    STATUS(SignalType.QUERY),
    
    /**
     * 指标查询信号：获取Actor性能指标
     */
    METRICS(SignalType.QUERY);

    private final SignalType type;

    Signal(SignalType type) {
        this.type = type;
    }

    /**
     * 获取信号类型
     */
    public SignalType getType() {
        return type;
    }

    /**
     * 判断是否为生命周期信号
     */
    public boolean isLifecycleSignal() {
        return type == SignalType.LIFECYCLE;
    }

    /**
     * 判断是否为控制信号
     */
    public boolean isControlSignal() {
        return type == SignalType.CONTROL;
    }

    /**
     * 判断是否为监督信号
     */
    public boolean isSupervisionSignal() {
        return type == SignalType.SUPERVISION;
    }

    /**
     * 判断是否为查询信号
     */
    public boolean isQuerySignal() {
        return type == SignalType.QUERY;
    }

}

