package com.avolution.actor.mailbox;

import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avolution.actor.message.Envelope;

/**
 * Actor的邮箱实现，支持优先级队列和消息暂存
 * 主要功能：
 * 1. 消息优先级处理
 * 2. 系统消息独立队列
 * 3. 消息暂存机制
 * 4. 并发安全处理
 * 5. 邮箱状态管理
 *
     // 1. 创建配置
     MailboxConfig config = MailboxConfig.builder()
     .capacity(2000)                // 设置较大的容量
     .throughputLimit(200)          // 适当的吞吐量限制
     .retryAttempts(5)             // 充分的重试次数
     .messageTimeout(60000)         // 合理的超时时间
     .build();

     // 2. 创建邮箱
     Mailbox mailbox = new Mailbox(config);

     // 3. 定期监控性能
     ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
     scheduler.scheduleAtFixedRate(() -> {
     MetricsSnapshot metrics = mailbox.getMetrics().getSnapshot();

     // 检查性能指标
     if (metrics.getErrors() > threshold) {
     logger.warn("High error rate detected: {}", metrics);
     }

     // 记录性能数据
     logger.info("Performance stats: {}", metrics);

     // 可能需要调整配置
     if (metrics.getOverflowMessages() > 0) {
     // 考虑增加容量或调整吞吐量
     }
     }, 0, 1, TimeUnit.MINUTES);

     // 4. 在应用关闭时清理资源
     Runtime.getRuntime().addShutdownHook(new Thread(() -> {
     scheduler.shutdown();
     mailbox.close();
     }));

 */
public class Mailbox {
    private static final Logger logger = LoggerFactory.getLogger(Mailbox.class);
    
    /**
     * 使用StampedLock替代ReentrantLock以提高性能
     */
    private final StampedLock mailboxLock = new StampedLock();
    
    /**
     * 主消息队列 - 使用自定义比较器的优先级队列
     */
    private volatile PriorityBlockingQueue<Envelope> messageQueue;
    
    /**
     * 系统消息和暂存队列
     */
    private final ConcurrentLinkedQueue<Envelope> systemQueue;
    private final ConcurrentLinkedQueue<Envelope> stashQueue;
    
    /**
     * 邮箱状态管理器
     * 控制邮箱的运行状态：运行、暂停、关闭
     */
    private final MailboxStatus status;
    
    /**
     * 消息计数器 - 跟踪当前邮箱中的消息总数
     */
    private final AtomicInteger messageCount;
    
    /**
     * 性能监控
     */
    private final MailboxMetrics metrics;
    
    /**
     * 配置参数
     */
    private volatile int capacity;
    private final boolean throughputEnabled;
    private volatile int throughputLimit;
    private final int retryAttempts;
    private final long retryDelayMs;

    /**
     * 创建指定容量的邮箱
     * @param config 邮箱配置
     */
    public Mailbox(MailboxConfig config) {
        this.capacity = config.getCapacity();
        this.throughputEnabled = config.isThroughputEnabled();
        this.throughputLimit = config.getThroughputLimit();
        this.retryAttempts = config.getRetryAttempts();
        this.retryDelayMs = config.getRetryDelayMs();
        
        this.messageQueue = createPriorityQueue(capacity);
        this.systemQueue = new ConcurrentLinkedQueue<>();
        this.stashQueue = new ConcurrentLinkedQueue<>();
        this.status = new MailboxStatus();
        this.messageCount = new AtomicInteger(0);
        this.metrics = new MailboxMetrics();
    }

    private PriorityBlockingQueue<Envelope> createPriorityQueue(int capacity) {
        return new PriorityBlockingQueue<>(
            capacity,
            Comparator.<Envelope>comparingInt(e -> e.getPriority().getValue())
                .reversed()
                .thenComparing(Envelope::getCreatedAt)
        );
    }

    /**
     * 发送消息到邮箱 - 修复潜在的死锁问题
     */
    public boolean enqueue(Envelope envelope) {
        // 先进行状态检查，避免在获取锁之前就可以返回
        if (status.isClosed()) {
            metrics.recordRejectedMessage();
            logger.warn("Mailbox closed, message rejected: {}", envelope);
            return false;
        }

        if (status.isSuspended()) {
            metrics.recordRejectedMessage();
            logger.warn("Mailbox suspended, message rejected: {}", envelope);
            return false;
        }

        // 使用乐观读检查容量
        long optimisticStamp = mailboxLock.tryOptimisticRead();
        boolean isFull = messageCount.get() >= capacity;
        if (mailboxLock.validate(optimisticStamp) && isFull) {
            metrics.recordOverflowMessage();
            logger.warn("Mailbox full (capacity: {}), message rejected: {}", capacity, envelope);
            return false;
        }

        // 获取写锁
        long stamp = mailboxLock.writeLock();
        try {
            boolean success = switch (envelope.getMessageType()) {
                case SYSTEM, SIGNAL -> {
                    metrics.recordSystemMessage();
                    yield systemQueue.offer(envelope);
                }
                default -> {
                    metrics.recordNormalMessage();
                    yield messageQueue.offer(envelope);
                }
            };

            if (success) {
                messageCount.incrementAndGet();
                metrics.recordSuccessfulEnqueue();
                logger.debug("Message enqueued: {}", envelope);
            }

            return success;
        } catch (Exception e) {
            metrics.recordError();
            logger.error("Error enqueueing message: {}", envelope, e);
            return handleEnqueueError(envelope, e);
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    /**
     * 获取下一个要处理的消息
     * @return 消息信封，如果没有消息或邮箱暂停则返回null
     */
    public Envelope dequeue() {
        long stamp = mailboxLock.readLock();
        try {
            // 检查邮箱状态
            if (status.isSuspended()) {
                return null;
            }
            // 优先处理系统消息
            Envelope envelope = systemQueue.poll();
            if (envelope != null) {
                messageCount.decrementAndGet();
                metrics.recordSystemMessageProcessed();
                return envelope;
            }
            // 处理普通消息
            envelope = messageQueue.poll();
            if (envelope != null) {
                messageCount.decrementAndGet();
                metrics.recordNormalMessageProcessed();
                return envelope;
            }
            return null;
        } finally {
            mailboxLock.unlockRead(stamp);
        }
    }

    /**
     * 批量处理消息
     * @return 处理的消息数量
     */
    public int processBatch() {
        if (!throughputEnabled) {
            return 0;
        }

        int processed = 0;
        long startTime = System.nanoTime();

        while (processed < throughputLimit) {
            Envelope envelope = dequeue();
            if (envelope == null) {
                break;
            }
            processed++;
            metrics.recordProcessingTime(System.nanoTime() - startTime);
        }

        return processed;
    }

    /**
     * 动态调整队列容量
     * @param newCapacity 新的队列容量
     */
    public void resizeCapacity(int newCapacity) {
        // 先进行参数验证
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("New capacity must be positive");
        }

        // 使用乐观读检查当前消息数量
        long optimisticStamp = mailboxLock.tryOptimisticRead();
        int currentCount = messageCount.get();
        if (mailboxLock.validate(optimisticStamp) && newCapacity < currentCount) {
            logger.warn("New capacity {} is less than current message count {}", 
                newCapacity, currentCount);
            return;
        }

        long stamp = mailboxLock.writeLock();
        try {
            // 再次验证容量
            if (newCapacity < messageCount.get()) {
                logger.warn("New capacity {} is less than current message count {}", 
                    newCapacity, messageCount.get());
                return;
            }

            PriorityBlockingQueue<Envelope> newQueue = createPriorityQueue(newCapacity);
            newQueue.addAll(messageQueue);
            this.messageQueue = newQueue;
            this.capacity = newCapacity;
            
            logger.info("Mailbox capacity adjusted to: {}", newCapacity);
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    private boolean handleEnqueueError(Envelope envelope, Exception e) {
        // 不在锁内重试，避免死锁
        for (int i = 0; i < retryAttempts; i++) {
            try {
                Thread.sleep(retryDelayMs);
                // 直接调用外部方法，而不是在锁内重试
                if (enqueue(envelope)) {
                    logger.info("Message enqueue retry succeeded after {} attempts", i + 1);
                    return true;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 暂存消息
     */
    public void stash(Envelope envelope) {
        long stamp = mailboxLock.writeLock();
        try {
            stashQueue.offer(envelope);
            metrics.recordStashedMessage();
            logger.debug("Message stashed: {}", envelope);
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    /**
     * 取出一个暂存的消息
     */
    public Envelope unstashOne() {
        long stamp = mailboxLock.writeLock();
        try {
            Envelope envelope = stashQueue.poll();
            if (envelope != null) {
                metrics.recordUnstashedMessage();
                logger.debug("Message unstashed: {}", envelope);
            }
            return envelope;
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    /**
     * 将所有暂存的消息重新放入主队列
     */
    public void unstashAll() {
        List<Envelope> stashedMessages = new ArrayList<>();
        
        // 首先获取所有暂存消息
        long stamp = mailboxLock.writeLock();
        try {
            Envelope envelope;
            while ((envelope = stashQueue.poll()) != null) {
                stashedMessages.add(envelope);
            }
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
        
        // 在锁外重新入队
        for (Envelope envelope : stashedMessages) {
            if (enqueue(envelope)) {
                logger.debug("Stashed message re-enqueued: {}", envelope);
            } else {
                logger.warn("Failed to re-enqueue stashed message: {}", envelope);
            }
        }
    }

    /**
     * 检查是否有暂存的消息
     * @return 是否有暂存消息
     */
    public boolean hasStashedMessages() {
        return !stashQueue.isEmpty();
    }

    /**
     * 获取暂存消息数量
     * @return 暂存消息数量
     */
    public int getStashedMessageCount() {
        return stashQueue.size();
    }

    /**
     * 清空邮箱所有队列
     */
    private void clear() {
        // 使用乐观读检查是否需要清理
        long optimisticStamp = mailboxLock.tryOptimisticRead();
        boolean isEmpty = messageCount.get() == 0;
        if (mailboxLock.validate(optimisticStamp) && isEmpty) {
            return;
        }

        messageQueue.clear();
        systemQueue.clear();
        stashQueue.clear();
        messageCount.set(0);
        metrics.recordClear();
        logger.debug("Mailbox cleared");
    }

    /**
     * 检查邮箱状态
     */
    public boolean hasMessages() {
        long stamp = mailboxLock.readLock();
        try {
            return !messageQueue.isEmpty() || !systemQueue.isEmpty();
        } finally {
            mailboxLock.unlockRead(stamp);
        }
    }

    /**
     * 获取当前消息数量
     */
    public int getMessageCount() {
        return messageCount.get();
    }

    /**
     * 检查邮箱是否为空
     */
    public boolean isEmpty() {
        return messageCount.get() == 0;
    }

    /**
     * 检查邮箱是否已满
     */
    public boolean isFull() {
        return messageCount.get() >= capacity;
    }

    /**
     * 获取邮箱状态
     */
    public MailboxStatus getStatus() {
        return status;
    }

    /**
     * 暂停邮箱处理
     */
    public void suspend() {
        long stamp = mailboxLock.writeLock();
        try {
            status.suspend();
            metrics.recordSuspend();
            logger.debug("Mailbox suspended");
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    /**
     * 恢复邮箱处理
     */
    public void resume() {
        long stamp = mailboxLock.writeLock();
        try {
            status.resume();
            metrics.recordResume();
            logger.debug("Mailbox resumed");
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    /**
     * 关闭邮箱
     */
    public void close() {
        long stamp = mailboxLock.writeLock();
        try {
            status.close();
            clear();
            metrics.recordClose();
            logger.debug("Mailbox closed");
        } finally {
            mailboxLock.unlockWrite(stamp);
        }
    }

    /**
     * 获取邮箱统计信息
     * @return 邮箱状态统计
     */
    public MailboxStats getStats() {
        return new MailboxStats(
            messageCount.get(),
            messageQueue.size(),
            systemQueue.size(),
            stashQueue.size(),
            status.isSuspended(),
            status.isClosed()
        );
    }

    /**
     * 获取性能监控信息
     * @return 性能监控
     */
    public MailboxMetrics getMetrics() {
        return metrics;
    }

    public long getMessageTimeout() {
        return 50L;
    }

    public int getThroughputLimit() {
        return throughputLimit;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }
}





