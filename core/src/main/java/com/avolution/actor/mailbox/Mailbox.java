package com.avolution.actor.mailbox;

import com.avolution.actor.message.Envelope;
import com.avolution.actor.message.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Actor的邮箱实现，支持优先级队列和消息暂存
 * 主要功能：
 * 1. 消息优先级处理
 * 2. 系统消息独立队列
 * 3. 消息暂存机制
 * 4. 并发安全处理
 * 5. 邮箱状态管理
 */
public class Mailbox {
    private static final Logger logger = LoggerFactory.getLogger(Mailbox.class);
    
    /**
     * 主消息队列 - 基于优先级的阻塞队列
     * 用于存储普通业务消息，支持按优先级和时间戳排序
     */
    private final PriorityBlockingQueue<Envelope> messageQueue;
    
    /**
     * 暂存队列 - 用于临时存储无法立即处理的消息
     * 当Actor状态允许时，可以重新处理这些消息
     */
    private final Queue<Envelope> stashQueue;
    
    /**
     * 系统消息队列 - 用于处理高优先级的系统控制消息
     * 如：生命周期信号、监控消息等
     */
    private final Queue<Envelope> systemQueue;
    
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
     * 邮箱锁 - 用于并发控制
     */
    private final ReentrantLock mailboxLock;
    
    // 配置参数
    private final int capacity;           // 邮箱容量
    private final boolean throughputEnabled;  // 是否启用吞吐量控制
    private final int throughputLimit;    // 单次处理消息数量限制

    /**
     * 创建指定容量的邮箱
     * @param capacity 邮箱最大容量
     */
    public Mailbox(int capacity) {
        this(capacity, true, 100);
    }

    /**
     * 创建自定义配置的邮箱
     * @param capacity 邮箱最大容量
     * @param throughputEnabled 是否启用吞吐量控制
     * @param throughputLimit 单次处理消息数量限制
     */
    public Mailbox(int capacity, boolean throughputEnabled, int throughputLimit) {
        // 初始化优先级队列，使用自定义比较器
        this.messageQueue = new PriorityBlockingQueue<>(
            capacity,
            Comparator.<Envelope>comparingInt(e -> e.getPriority().getValue())
                .reversed()  // 高优先级在前
                .thenComparing(Envelope::getCreatedAt)  // 同优先级按时间排序
        );
        
        this.stashQueue = new ConcurrentLinkedQueue<>();
        this.systemQueue = new ConcurrentLinkedQueue<>();
        this.status = new MailboxStatus();
        this.messageCount = new AtomicInteger(0);
        this.mailboxLock = new ReentrantLock();
        
        this.capacity = capacity;
        this.throughputEnabled = throughputEnabled;
        this.throughputLimit = throughputLimit;
    }

    /**
     * 发送消息到邮箱
     * @param envelope 消息信封
     * @return 是否成功入队
     */
    public boolean enqueue(Envelope envelope) {
        try {
            mailboxLock.lock();
            
            // 检查邮箱状态
            if (status.isClosed()) {
                logger.warn("邮箱已关闭，拒绝消息: {}", envelope);
                return false;
            }

            // 检查容量
            if (messageCount.get() >= capacity) {
                logger.warn("邮箱已满，容量: {}", capacity);
                return false;
            }

            // 根据消息类型选择队列
            boolean success = switch (envelope.getMessageType()) {
                case SYSTEM, SIGNAL -> systemQueue.offer(envelope);
                default -> messageQueue.offer(envelope);
            };

            if (success) {
                messageCount.incrementAndGet();
                logger.debug("消息已入队: {}", envelope);
            }

            return success;
        } finally {
            mailboxLock.unlock();
        }
    }

    /**
     * 获取下一个要处理的消息
     * @return 消息信封，如果没有消息或邮箱暂停则返回null
     */
    public Envelope dequeue() {
        try {
            mailboxLock.lock();
            
            if (status.isSuspended()) {
                return null;
            }

            // 优先处理系统消息
            Envelope envelope = systemQueue.poll();
            if (envelope != null) {
                messageCount.decrementAndGet();
                logger.debug("处理系统消息: {}", envelope);
                return envelope;
            }

            // 处理普通消息
            envelope = messageQueue.poll();
            if (envelope != null) {
                messageCount.decrementAndGet();
                logger.debug("处理普通消息: {}", envelope);
                return envelope;
            }

            return null;
        } finally {
            mailboxLock.unlock();
        }
    }

    /**
     * 批量处理消息
     * @param maxMessages 最大处理消息数
     * @return 处理的消息数量
     */
    public int processBatch(int maxMessages) {
        int processed = 0;
        while (processed < maxMessages) {
            Envelope envelope = dequeue();
            if (envelope == null) {
                break;
            }
            processed++;
        }
        return processed;
    }

    /**
     * 暂存消息
     * @param envelope 要暂存的消息
     */
    public void stash(Envelope envelope) {
        try {
            mailboxLock.lock();
            stashQueue.offer(envelope);
            logger.debug("消息已暂存: {}", envelope);
        } finally {
            mailboxLock.unlock();
        }
    }

    /**
     * 取出一个暂存的消息
     * @return 暂存的消息，如果没有则返回null
     */
    public Envelope unstashOne() {
        try {
            mailboxLock.lock();
            Envelope envelope = stashQueue.poll();
            if (envelope != null) {
                logger.debug("取出暂存消息: {}", envelope);
            }
            return envelope;
        } finally {
            mailboxLock.unlock();
        }
    }

    /**
     * 将所有暂存的消息重新放入主队列
     */
    public void unstashAll() {
        try {
            mailboxLock.lock();
            Envelope envelope;
            while ((envelope = stashQueue.poll()) != null) {
                enqueue(envelope);
                logger.debug("暂存消息重新入队: {}", envelope);
            }
        } finally {
            mailboxLock.unlock();
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
    public void clear() {
        try {
            mailboxLock.lock();
            messageQueue.clear();
            stashQueue.clear();
            systemQueue.clear();
            messageCount.set(0);
            logger.debug("邮箱已清空");
        } finally {
            mailboxLock.unlock();
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
        status.suspend();
        logger.debug("Mailbox suspended");
    }

    /**
     * 恢复邮箱处理
     */
    public void resume() {
        status.resume();
        logger.debug("Mailbox resumed");
    }

    /**
     * 关闭邮箱
     */
    public void close() {
        status.close();
        clear();
        logger.debug("Mailbox closed");
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
}

/**
 * 邮箱统计信息类
 */
class MailboxStats {
    private final int totalMessages;
    private final int normalMessages;
    private final int systemMessages;
    private final int stashedMessages;
    private final boolean suspended;
    private final boolean closed;

    public MailboxStats(int totalMessages, int normalMessages, 
                       int systemMessages, int stashedMessages,
                       boolean suspended, boolean closed) {
        this.totalMessages = totalMessages;
        this.normalMessages = normalMessages;
        this.systemMessages = systemMessages;
        this.stashedMessages = stashedMessages;
        this.suspended = suspended;
        this.closed = closed;
    }

    // Getters...
}

/**
 * 邮箱状态类
 */
class MailboxStatus {
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