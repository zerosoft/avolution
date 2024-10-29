package com.avolution.actor.strategy;

public interface SupervisorStrategy {

    enum DirectiveType {
        RESUME,    // Resume the child, keeping its accumulated internal state
        RESTART,   // Restart the child, clearing out its accumulated internal state
        STOP,      // Stop the child permanently
        ESCALATE   // Escalate the failure to the parent actor
    }

    DirectiveType decide(Throwable t);
}
