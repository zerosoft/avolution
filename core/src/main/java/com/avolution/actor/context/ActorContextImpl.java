package com.avolution.actor.context;

import com.avolution.actor.core.*;
import com.avolution.actor.impl.;
import com.avolution.actor.supervision.SupervisorStrategy;
import com.avolution.actor.dispatch.Dispatcher;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;

public class ActorContextImpl implements ActorContext {

    private final IActorSystem system;

    private final ActorRef self;
    private final ActorRef parent;

    private final Set<ActorRef> children;
    private final AtomicReference<ActorRef> currentSender;
    private final SupervisorStrategy supervisorStrategy;
    private final Dispatcher dispatcher;

    public ActorContextImpl(IActorSystem system,
                          ActorRef self,
                          ActorRef parent,
                          SupervisorStrategy supervisorStrategy,
                          Dispatcher dispatcher) {
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.supervisorStrategy = supervisorStrategy;
        this.dispatcher = dispatcher;
        this.children = ConcurrentHashMap.newKeySet();
        this.currentSender = new AtomicReference<>();
    }

    @Override
    public ActorRef self() {
        return self;
    }

    @Override
    public ActorRef parent() {
        return parent;
    }

    @Override
    public IActorSystem system() {
        return system;
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    @Override
    public ActorRef actorOf(Props props, String name) {
        // 验证actor名称
        validateActorName(name);
        
        // 构建完整路径
        String childPath = buildChildPath(name);
        
        // 检查是否已存在
        if (findChild(name) != null) {
            throw new IllegalArgumentException("Child with name " + name + " already exists");
        }

        try {
            // 创建子Actor
            Actor childActor = props.newActor();
            
            // 创建子Actor的上下文
            ActorContextImpl childContext = new ActorContextImpl(
                system,
                null, // 将在ActorRefImpl构造时设置
                self,
                childActor,
                props.getSupervisorStrategy(),
                dispatcher
            );

            // 创建ActorRef
            ActorRefImpl childRef = new ActorRefImpl(
                childPath,
                childActor,
                childContext,
                dispatcher
            );
            
            // 设置双向引用
            childContext.setSelf(childRef);
            childActor.setContext(childContext);

            // 添加到子Actor集合
            children.add(childRef);

            // 调用生命周期方法
            dispatcher.execute(() -> childActor.preStart());

            return childRef;
        } catch (Exception e) {
            throw new ActorInitializationException("Failed to create child actor", e);
        }
    }

    @Override
    public void stop(ActorRef child) {
        if (children.remove(child)) {
            child.stop();
            // 通知监督者
            if (supervisorStrategy != null) {
                handleChildTermination(child);
            }
        }
    }

    @Override
    public ActorRef sender() {
        return currentSender.get();
    }

    @Override
    public Iterable<ActorRef> getChildren() {
        return Set.copyOf(children);
    }

    @Override
    public String path() {
        return self.path();
    }

    // 内部方法
    public void setCurrentSender(ActorRef sender) {
        currentSender.set(sender);
    }

    private void setSelf(ActorRef self) {
        if (this.self != null) {
            throw new IllegalStateException("Self reference already set");
        }
        // 使用反射设置final字段
        try {
            var field = this.getClass().getDeclaredField("self");
            field.setAccessible(true);
            field.set(this, self);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set self reference", e);
        }
    }

    public void handleChildFailure(ActorRef child, Throwable cause) {
        if (supervisorStrategy != null) {
            SupervisorStrategy.SupervisorDirective directive = 
                supervisorStrategy.handle(cause);
            
            switch (directive) {
                case RESUME:
                    // 继续处理下一条消息
                    break;
                case RESTART:
                    restartChild(child, cause);
                    break;
                case STOP:
                    stop(child);
                    break;
                case ESCALATE:
                    escalateFailure(cause);
                    break;
            }
        }
    }

    private void handleChildTermination(ActorRef child) {
        // 可以添加额外的清理逻辑
        children.remove(child);
    }

    private void restartChild(ActorRef child, Throwable cause) {
        if (child instanceof ActorRefImpl actorRef) {
            dispatcher.execute(() -> {
                try {
                    Actor childActor = actorRef.getActor();
                    childActor.preRestart(cause);
                    childActor.postRestart(cause);
                } catch (Exception e) {
                    escalateFailure(e);
                }
            });
        }
    }

    private void escalateFailure(Throwable cause) {
        if (parent != null) {
            parent.tell(new Failed(cause, self), self);
        } else {
            // 根actor处理失败
            system.stop(self);
        }
    }

    private ActorRef findChild(String name) {
        String childPath = buildChildPath(name);
        return children.stream()
            .filter(child -> child.path().equals(childPath))
            .findFirst()
            .orElse(null);
    }

    private String buildChildPath(String name) {
        return path() + File.separator + name;
    }

    private void validateActorName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Actor name must not be empty");
        }
        if (name.contains("/") || name.contains(":")) {
            throw new IllegalArgumentException("Actor name must not contain '/' or ':'");
        }
    }

    // 内部消息类
    public static class Failed {
        public final Throwable cause;
        public final ActorRef child;

        public Failed(Throwable cause, ActorRef child) {
            this.cause = cause;
            this.child = child;
        }
    }

    public static class ActorInitializationException extends RuntimeException {
        public ActorInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 