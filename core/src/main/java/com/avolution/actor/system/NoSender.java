package com.avolution.actor.system;


import com.avolution.actor.core.ActorRef;
import com.avolution.actor.message.Signal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 表示空发送者的ActorRef实现
 */
public final class NoSender implements ActorRef<Object> {

    private static final NoSender INSTANCE = new NoSender();
    private static final String NO_SENDER_PATH = "noSender";

    private NoSender() {
        // 私有构造函数防止外部实例化
    }

    public static ActorRef noSender() {
        return INSTANCE;
    }

    @Override
    public void tell(Object message, ActorRef sender) {
        throw new UnsupportedOperationException("NoSender cannot send messages");
    }

    @Override
    public void tell(Signal signal, ActorRef sender) {
        throw new UnsupportedOperationException("NoSender cannot send signals");
    }

    @Override
    public <R> CompletableFuture<R> ask(Object message, Duration timeout) {
        throw new UnsupportedOperationException("NoSender cannot ask messages");
    }

    @Override
    public String path() {
        return NO_SENDER_PATH;
    }

    @Override
    public String name() {
        return NO_SENDER_PATH;
    }

    @Override
    public boolean isTerminated() {
        return true;
    }

    @Override
    public String toString() {
        return "NoSender";
    }
}