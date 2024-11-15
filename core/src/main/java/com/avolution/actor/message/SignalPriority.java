package com.avolution.actor.message;

public enum SignalPriority {
    HIGH(0),
    NORMAL(1),
    LOW(2);

    public final int value;

    SignalPriority(int value) {
        this.value = value;
    }
}
