package com.avolution.actor.context;


import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;
import com.avolution.actor.message.MessageType;
import com.avolution.actor.system.actor.IDeadLetterActorMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ActorContextManager {
    private final ConcurrentHashMap<String, ActorContext> contexts;
    private final ActorSystem system;

    public ActorContextManager(ActorSystem system) {
        this.contexts = new ConcurrentHashMap<>();
        this.system = system;
    }

    public void addContext(String path, ActorContext context) {
        contexts.put(path, context);
    }

    public Optional<ActorContext> getContext(String path) {
        return Optional.ofNullable(contexts.get(path));
    }

    public void removeContext(String path) {
        contexts.remove(path);
    }

    public Set<ActorContext> getChildContexts(String parentPath) {
        return contexts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(parentPath + "/"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    public void terminateAll() {
        contexts.values().forEach(context -> {
            try {
                context.stop();
            } catch (Exception e) {
                // 记录错误但继续终止其他上下文
                system.getDeadLetters().tell(new IDeadLetterActorMessage.DeadLetter("Error terminating actor", "", context.getPath(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), MessageType.SYSTEM.toString(), 0), ActorRef.noSender());
            }
        });
        contexts.clear();
    }

    public boolean hasContext(String path) {
        return contexts.containsKey(path);
    }

    public int getContextCount() {
        return contexts.size();
    }

    public void suspendContext(String path) {
        getContext(path).ifPresent(context -> {
            context.mailbox.suspend();
        });
    }

    public void resumeContext(String path) {
        getContext(path).ifPresent(context -> {
            context.mailbox.resume();
        });
    }
}