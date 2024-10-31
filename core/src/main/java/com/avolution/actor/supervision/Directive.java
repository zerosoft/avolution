package com.avolution.actor.supervision;

public enum Directive {
    RESUME,    // 继续处理下一条消息
    RESTART,   // 重启Actor
    STOP,      // 停止Actor
    ESCALATE   // 向上传递错误
}
