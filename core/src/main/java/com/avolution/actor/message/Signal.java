package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;


//public sealed interface Signal permits PoisonPill, ReceiveTimeout, Restart, StopMessage, SupervisionMessage, SystemStopMessage, Terminated {
//    void handle(AbstractActor<?> actor);
//}


public enum Signal {


    // 生命周期信号
    START(SignalType.LIFECYCLE),
    STOP(SignalType.LIFECYCLE),
    RESTART(SignalType.LIFECYCLE),
    SUSPEND(SignalType.CONTROL),
    RESUME(SignalType.CONTROL),

    // 系统控制信号
    POISON_PILL(SignalType.CONTROL),
    KILL(SignalType.CONTROL),

    // 监督信号
    ESCALATE(SignalType.SUPERVISION),
    SUPERVISE(SignalType.SUPERVISION),

    // 查询信号
    STATUS(SignalType.QUERY),
    METRICS(SignalType.QUERY);

    private final SignalType type;

    Signal(SignalType type) {
        this.type = type;
    }

    public SignalType getType() {
        return type;
    }

    public boolean isLifecycleSignal() {
        return type == SignalType.LIFECYCLE;
    }

    public boolean isControlSignal() {
        return type == SignalType.CONTROL;
    }

    public boolean isSupervisionSignal() {
        return type == SignalType.SUPERVISION;
    }

    public boolean isQuerySignal() {
        return type == SignalType.QUERY;
    }

    public SignalEnvelope envelope(ActorRef<?> sender, ActorRef<Signal> recipient) {
        return new SignalEnvelope(this, sender, recipient);
    }

    public SignalEnvelope envelope(ActorRef<?> sender, ActorRef<Signal> recipient,
                                   SignalScope scope) {
        return new SignalEnvelope(this, sender, recipient,SignalPriority.NORMAL, scope);
    }
}

