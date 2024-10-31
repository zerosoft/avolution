package com.avolution.actor.lifecycle;

import com.avolution.actor.core.ActorRef;

public sealed interface Lifecycle {
    record ChildCreated(ActorRef child) implements Lifecycle {
    }

    record ChildTerminated(ActorRef child) implements Lifecycle {
    }

    record PreStart() implements Lifecycle {
    }

    record PostStop() implements Lifecycle {
    }

    record PreRestart(Throwable cause) implements Lifecycle {
    }

    record PostRestart(Throwable cause) implements Lifecycle {
    }
}