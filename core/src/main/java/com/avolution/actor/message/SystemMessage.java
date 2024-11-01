package com.avolution.actor.message;

import com.avolution.actor.core.ActorRef;

public class SystemMessage {
    public static class Start {}
    public static class PoisonPill {}
    public static class AutoDestroyMessage {}
    
    public static class Failed {
        public final ActorRef actor;
        public final Throwable error;

        public Failed(ActorRef actor, Throwable error) {
            this.actor = actor;
            this.error = error;
        }
    }
}

/**
 * 系统消息类型枚举
 */
enum SystemMessageType {
    STOP,
    RESTART,
    SUSPEND,
    RESUME
}

/**
 * 停止消息
 */
class StopMessage extends SystemMessage {
    public StopMessage() {
        super(SystemMessageType.STOP);
    }
}

/**
 * 重启消息
 */
class RestartMessage extends SystemMessage {
    public RestartMessage() {
        super(SystemMessageType.RESTART);
    }
} 