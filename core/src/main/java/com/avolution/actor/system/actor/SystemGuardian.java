package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.SystemMessage.Failed;

public class SystemGuardian extends AbstractActor {
    @Override
    public void onReceive(Envelope envelope) {
        if (envelope.getMessage() instanceof Failed) {
            Failed failed = (Failed) envelope.getMessage();
            handleSystemFailure(failed.actor, failed.error);
        }
    }

    protected void handleSystemFailure(ActorRef actor, Throwable error) {
        // 实现系统Actor失败处理逻辑
    }
} 