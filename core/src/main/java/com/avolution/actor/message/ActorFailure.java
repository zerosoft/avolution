package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

/**
 * Actor失败消息，用于向监督者报告Actor的失败状态
 */
public class ActorFailure {
    private final ActorRef failedActor;
    private final Throwable cause;
    private final Envelope failedMessage;
    private final long timestamp;

    public ActorFailure(ActorRef failedActor, Throwable cause, Envelope failedMessage) {
        this.failedActor = failedActor;
        this.cause = cause;
        this.failedMessage = failedMessage;
        this.timestamp = System.currentTimeMillis();
    }

    public ActorRef getFailedActor() {
        return failedActor;
    }

    public Throwable getCause() {
        return cause;
    }

    public Envelope getFailedMessage() {
        return failedMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(
            "ActorFailure[actor=%s, cause=%s, message=%s, timestamp=%d]",
            failedActor, cause.getMessage(), failedMessage, timestamp
        );
    }
} 