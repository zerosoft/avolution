package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.SystemMessage.Failed;

public class UserGuardian extends AbstractActor {


    public void onReceive(Envelope envelope) {
        if (envelope.getMessage() instanceof Failed) {
            Failed failed = (Failed) envelope.getMessage();
            handleUserFailure(failed.actor, failed.error);
        }
    }

    protected void handleUserFailure(ActorRef actor, Throwable error) {
        // 实现用户Actor失败处理逻辑
    }
}