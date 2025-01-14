package com.avolution.actor.core;

import com.avolution.actor.core.context.ActorContext;
import com.avolution.actor.core.lifecycle.ActorLifecycleHook;
import com.avolution.actor.message.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类型安全的Actor基类
 * @param <T> 消息类型参数
 */
public abstract class TypedActor<T> implements ActorLifecycleHook {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    // Actor上下文
    private ActorContext actorContext;
    // 当前消息信封
    private Envelope currentEnvelope;
    /**
     * 处理消息
     */
    public void receive(Envelope envelope) {
        if (envelope == null || envelope.getMessage() == null) {
            logger.warn("Received null envelope or message");
            return;
        }
        currentEnvelope = envelope;
        Object message = envelope.getMessage();

        try {
            onReceive((T) message);
        } catch (Exception e) {
            handleError(e, envelope);
        } finally {
            currentEnvelope = null;
        }

    }

    /**
     * 处理错误
     */
    protected void handleError(Throwable error, Envelope envelope) {
        logger.error("Error processing message: {}", envelope, error);

        // 创建错误回复
        if (envelope.getSender() != null) {
            Envelope errorReply = envelope.createErrorReply(error);
            getContext().tell(errorReply);
        }

        // 通知监督者
        getContext().escalate(error, envelope);
    }


    // Context management methods
    public void setActorContext(ActorContext context) {
        this.actorContext = context;
    }

    public  ActorContext getContext() {
        return actorContext;
    }

    public ActorRef getSelf() {
        return getContext().getUnTypedActor().getSelfRef();
    }

    public ActorRef getSender() {
        return getContext().getUnTypedActor().getSender();
    }

    public String getPath() {
        return getContext().getPath();
    }

    public Envelope getCurrentEnvelope() {
        return currentEnvelope;
    }

    /**
     * 抽象的消息处理方法
     * 子类必须实现此方法来处理特定类型的消息
     */
    protected abstract void onReceive(T message) throws Exception;
}