package com.avolution.actor.core;

import com.avolution.actor.lifecycle.LifecycleState;

public record ActorStatus(
        String actorPath,
        LifecycleState state,
        int childCount,
        boolean processing
) {}