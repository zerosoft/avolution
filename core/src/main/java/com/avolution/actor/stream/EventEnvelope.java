package com.avolution.actor.stream;

import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.MessageType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventEnvelope extends Envelope {

    private EventEnvelope(Builder builder) {
        super(builder.event,null,null, MessageType.SYSTEM,0,builder.priority);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Object getEvent() {
        return getMessage();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata());
    }

    public static class Builder {
        private Object event;
        private Priority priority = Priority.NORMAL;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder event(Object event) {
            this.event = event;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public EventEnvelope build() {
            if (event == null) {
                throw new IllegalStateException("Event cannot be null");
            }
            return new EventEnvelope(this);
        }
    }
}