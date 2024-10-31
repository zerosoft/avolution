package com.avolution.actor.message;

import com.avolution.actor.core.Actor;
import com.avolution.actor.core.ActorRef;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class DeadLetterOffice extends Actor {
    private final Queue<DeadLetter> recentDeadLetters;
    private final int maxStoredDeadLetters;
    private final AtomicLong totalDeadLetters = new AtomicLong(0);

    public DeadLetterOffice(int maxStoredDeadLetters) {
        this.maxStoredDeadLetters = maxStoredDeadLetters;
        this.recentDeadLetters = new LinkedList<>();
    }

    @Override
    protected void receive(Object message) {
        if (message instanceof DeadLetter deadLetter) {
            handleDeadLetter(deadLetter);
        } else if (message instanceof GetDeadLetterStats) {
            handleGetStats();
        } else if (message instanceof ClearDeadLetters) {
            handleClear();
        }
    }

    private void handleDeadLetter(DeadLetter deadLetter) {
        totalDeadLetters.incrementAndGet();

        synchronized (recentDeadLetters) {
            recentDeadLetters.offer(deadLetter);
            while (recentDeadLetters.size() > maxStoredDeadLetters) {
                recentDeadLetters.poll();
            }
        }
    }

    private void handleGetStats() {
        DeadLetterStats stats = new DeadLetterStats(
                totalDeadLetters.get(),
                new LinkedList<>(recentDeadLetters)
        );
        getContext().sender().tell(stats, getContext().self());
    }

    private void handleClear() {
        synchronized (recentDeadLetters) {
            recentDeadLetters.clear();
        }
        totalDeadLetters.set(0);
    }

    // 消息类
    public static class GetDeadLetterStats {
    }

    public static class ClearDeadLetters {
    }

    public record DeadLetterStats(
            long totalDeadLetters,
            Queue<DeadLetter> recentDeadLetters
    )
}