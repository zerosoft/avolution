package com.avolution.actor.mailbox;

/**
 * MailboxStats 类用于表示邮箱的统计信息。
 * 它包含了邮箱中消息的总数、普通消息数量、系统消息数量、暂存消息数量，
 * 以及邮箱是否被挂起和是否已关闭的状态。
 */
public class MailboxStats {
    // 邮箱中消息的总数
    private final int totalMessages;
    // 普通消息的数量
    private final int normalMessages;
    // 系统消息的数量
    private final int systemMessages;
    // 暂存消息的数量
    private final int stashedMessages;
    // 邮箱是否被挂起
    private final boolean suspended;
    // 邮箱是否已关闭
    private final boolean closed;

    /**
     * 构造函数，用于初始化 MailboxStats 对象。
     *
     * @param totalMessages    邮箱中消息的总数
     * @param normalMessages   普通消息的数量
     * @param systemMessages   系统消息的数量
     * @param stashedMessages  暂存消息的数量
     * @param suspended        邮箱是否被挂起
     * @param closed           邮箱是否已关闭
     */
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

    /**
     * 获取邮箱中消息的总数。
     *
     * @return 消息总数
     */
    public int getTotalMessages() {
        return totalMessages;
    }

    /**
     * 获取普通消息的数量。
     *
     * @return 普通消息数量
     */
    public int getNormalMessages() {
        return normalMessages;
    }

    /**
     * 获取系统消息的数量。
     *
     * @return 系统消息数量
     */
    public int getSystemMessages() {
        return systemMessages;
    }

    /**
     * 获取暂存消息的数量。
     *
     * @return 暂存消息数量
     */
    public int getStashedMessages() {
        return stashedMessages;
    }

    /**
     * 判断邮箱是否被挂起。
     *
     * @return 如果邮箱被挂起，则返回 true；否则返回 false
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * 判断邮箱是否已关闭。
     *
     * @return 如果邮箱已关闭，则返回 true；否则返回 false
     */
    public boolean isClosed() {
        return closed;
    }
}
