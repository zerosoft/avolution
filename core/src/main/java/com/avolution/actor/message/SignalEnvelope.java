package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

public class SignalEnvelope extends Envelope<Signal> {

    private final SignalPriority priority;
    private final SignalScope scope;

    public SignalEnvelope(Signal signal, ActorRef<?> sender, ActorRef<Signal> recipient) {
        this(signal, sender, recipient, determineSignalPriority(signal), SignalScope.SINGLE);
    }

    public SignalEnvelope(Signal signal, ActorRef<?> sender, ActorRef<Signal> recipient,SignalPriority priority, SignalScope scope) {
        super(signal, sender, recipient, MessageType.SYSTEM, 0);
        this.priority = priority;
        this.scope = scope;
    }

    private static SignalPriority determineSignalPriority(Signal signal) {
        if (signal == Signal.KILL || signal == Signal.ESCALATE) {
            return SignalPriority.HIGH;
        } else if (signal.isLifecycleSignal()) {
            return SignalPriority.NORMAL;
        } else {
            return SignalPriority.LOW;
        }
    }

    public SignalPriority priority() {
        return priority;
    }

    public SignalScope scope() {
        return scope;
    }

    @Override
    public boolean isSystemMessage() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("SignalEnvelope[signal=%s, priority=%s, scope=%s]",
                getMessage(), priority, scope);
    }
}

