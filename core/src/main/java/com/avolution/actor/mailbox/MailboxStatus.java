package com.avolution.actor.mailbox;

public class MailboxStatus {
    private volatile boolean suspended = false;
    private volatile boolean closed = false;

    public boolean isSuspended() {
        return suspended;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        suspended = false;
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        closed = true;
    }
}
