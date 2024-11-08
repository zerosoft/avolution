package com.avolution.actor.system.actor;

import com.avolution.actor.core.AbstractActor;
import com.avolution.actor.core.annotation.OnReceive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DeadLetterActor extends AbstractActor<IDeadLetterActorMessage> {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);

    // 保存最近的死信消息
    private final ConcurrentLinkedQueue<IDeadLetterActorMessage.DeadLetter> recentDeadLetters = new ConcurrentLinkedQueue<>();
    private static final int MAX_DEAD_LETTERS = 1000;
    private final AtomicInteger deadLetterCount = new AtomicInteger(0);

    @OnReceive(IDeadLetterActorMessage.DeadLetter.class)
    private void handleDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        // 记录死信
        log.warn("Dead letter received: {}", deadLetter);

        // 维护最近的死信队列
        recentDeadLetters.offer(deadLetter);
        if (recentDeadLetters.size() > MAX_DEAD_LETTERS) {
            recentDeadLetters.poll();
        }

        // 统计死信数量
        int count = deadLetterCount.incrementAndGet();
        if (count % 100 == 0) {
            log.warn("Dead letter count reached: {}", count);
        }

        // 根据死信类型进行特殊处理
        handleSpecificDeadLetter(deadLetter);
    }

    private void handleSpecificDeadLetter(IDeadLetterActorMessage.DeadLetter deadLetter) {
        if (deadLetter.retryCount() > 0) {
            log.warn("Message retry failed after {} attempts: {}",
                    deadLetter.retryCount(), deadLetter.message());
        }

        // 可以根据消息类型添加特殊处理逻辑
        if (deadLetter.messageType().equals("SYSTEM")) {
            log.error("System message became dead letter: {}", deadLetter);
        }
    }

    @Override
    public void preStart() {
        log.info("DeadLetterActor started at: {}", path());
    }

    @Override
    protected void onPostStop() {
        log.info("DeadLetterActor stopped. Total dead letters processed: {}",
                deadLetterCount.get());
        recentDeadLetters.clear();
    }

    // 提供查询接口
    public int getDeadLetterCount() {
        return deadLetterCount.get();
    }

    public ConcurrentLinkedQueue<IDeadLetterActorMessage.DeadLetter> getRecentDeadLetters() {
        return new ConcurrentLinkedQueue<>(recentDeadLetters);
    }
}