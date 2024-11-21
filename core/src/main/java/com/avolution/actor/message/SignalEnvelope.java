package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

public class SignalEnvelope extends Envelope {

    private final SignalScope scope;

    private SignalEnvelope(Builder builder) {
        super(builder.signal, builder.sender, builder.receiver, MessageType.SIGNAL,0, builder.priority);
        this.scope = builder.scope;
    }

    public static Builder builder() {
        return new Builder();
    }

    public SignalScope getScope() {
        return scope;
    }

    public Signal getSignal() {
        return (Signal) getMessage();
    }

    public static class Builder {
        private Signal signal;
        private ActorRef<?> sender;
        private ActorRef<Signal> receiver;
        private Priority priority = Priority.NORMAL;
        private SignalScope scope = SignalScope.SINGLE;

        public Builder signal(Signal signal) {
            this.signal = signal;
            return this;
        }

        public Builder sender(ActorRef<?> sender) {
            this.sender = sender;
            return this;
        }

        public Builder receiver(ActorRef<Signal> receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder scope(SignalScope scope) {
            this.scope = scope;
            return this;
        }

        public SignalEnvelope build() {
            if (signal == null) {
                throw new IllegalStateException("Signal must be set");
            }
//            if (receiver == null) {
//                throw new IllegalStateException("Receiver must be set");
//            }
            return new SignalEnvelope(this);
        }
    }

    @Override
    public boolean isSystemMessage() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("SignalEnvelope[signal=%s, priority=%s, scope=%s]",
                getMessage(), getPriority(), scope);
    }
}

