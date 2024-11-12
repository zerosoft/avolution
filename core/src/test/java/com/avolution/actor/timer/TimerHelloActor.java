package com.avolution.actor.timer;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.annotation.OnReceive;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerHelloActor extends AbstractActor<TimerHelloActor.Message> {
    private final AtomicInteger timerCount = new AtomicInteger(0);
    private static final String TIMER_KEY = "hello-timer";

    public interface Message {}

    public static class StartTimer implements Message {
        private final Duration interval;

        public StartTimer(Duration interval) {
            this.interval = interval;
        }

        public Duration getInterval() {
            return interval;
        }
    }

    public static class TimerTick implements Message {
        private final int count;

        public TimerTick(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

    public static class GetTimerCount implements Message {}

    @OnReceive(StartTimer.class)
    private void onStartTimer(StartTimer msg) {
        context.scheduleRepeatedly(
                TIMER_KEY,
                Duration.ZERO,
                msg.getInterval(),
                new TimerTick(timerCount.incrementAndGet())
        );
    }

    @OnReceive(TimerTick.class)
    private void onTimerTick(TimerTick msg) {
        System.out.println("Timer tick: " + msg.getCount());
        timerCount.incrementAndGet();
    }

    @OnReceive(GetTimerCount.class)
    private void onGetTimerCount(GetTimerCount msg) {
        getSender().tell(timerCount.get(), getSelf());
    }

    @Override
    public void onPostStop() {
        context.cancelTimer(TIMER_KEY);
        super.onPostStop();
    }
}