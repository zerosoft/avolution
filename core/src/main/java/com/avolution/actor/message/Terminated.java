package com.avolution.actor.message;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Terminated implements Signal {

    private final String actorPath;
    private final String reason;
    private final boolean expected;

    public Terminated() {
        this(null, "Normal termination", true);
    }

    public Terminated(String actorPath, String reason, boolean expected) {
        this.actorPath = actorPath;
        this.reason = reason;
        this.expected = expected;
    }

    public String getActorPath() {
        return actorPath;
    }

    public String getReason() {
        return reason;
    }

    public boolean isExpected() {
        return expected;
    }


    @Override
    public String toString() {
        return String.format("Terminated[actor=%s, reason=%s, expected=%s]",
                actorPath, reason, expected);
    }

    @Override
    public void handle(AbstractActor<?> target) {
        if (target instanceof AbstractActor<?> actor) {
            // 通知死信系统
            if (actor.getContext() != null && actor.getContext().system() != null) {
                IDeadLetterActorMessage.DeadLetter deadLetter = new IDeadLetterActorMessage.DeadLetter(
                        this,
                        actor.getSelf().path(),
                        actorPath != null ? actorPath : "unknown",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        MessageType.SYSTEM.toString(),
                        0
                );
                actor.getContext().system().getDeadLetters().tell(deadLetter, actor.getSelf());
            }

            // 通知监视器
            actor.getContext().notifyWatchers(target);

            // 如果是非预期终止，记录日志
            if (!expected) {
//                actor.logger.warn("Actor terminated unexpectedly: {} - Reason: {}",
//                        actorPath, reason);
            }
        }
    }
}