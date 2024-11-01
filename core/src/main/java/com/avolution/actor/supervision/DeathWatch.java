package com.avolution.actor.supervision;

import com.avolution.actor.core.ActorRef;
import com.avolution.actor.core.ActorSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Actor死亡监视器
 */
public class DeathWatch {
    private final ActorSystem system;
    private final Map<ActorRef, Set<ActorRef>> watchedBy;  // actor -> watchers
    private final Map<ActorRef, Set<ActorRef>> watching;   // watcher -> watched actors

    public DeathWatch(ActorSystem system) {
        this.system = system;
        this.watchedBy = new ConcurrentHashMap<>();
        this.watching = new ConcurrentHashMap<>();
    }

    /**
     * 注册监视关系
     */
    public void watch(ActorRef watcher, ActorRef watched) {
        if (watcher == watched) return;  // 不能自己监视自己

        watchedBy.computeIfAbsent(watched, k -> ConcurrentHashMap.newKeySet())
                .add(watcher);
        watching.computeIfAbsent(watcher, k -> ConcurrentHashMap.newKeySet())
                .add(watched);
    }

    /**
     * 取消监视关系
     */
    public void unwatch(ActorRef watcher, ActorRef watched) {
        Optional.ofNullable(watchedBy.get(watched))
                .ifPresent(watchers -> watchers.remove(watcher));
        Optional.ofNullable(watching.get(watcher))
                .ifPresent(watchedActors -> watchedActors.remove(watched));
    }

    /**
     * 处理Actor终止
     */
    public void terminated(ActorRef actor) {
        // 通知所有监视者
        Optional.ofNullable(watchedBy.remove(actor))
                .ifPresent(watchers ->
                        watchers.forEach(watcher ->
                                notifyWatcher(watcher, actor)));

        // 清理监视关系
        Optional.ofNullable(watching.remove(actor))
                .ifPresent(watchedActors ->
                        watchedActors.forEach(watched ->
                                Optional.ofNullable(watchedBy.get(watched))
                                        .ifPresent(ws -> ws.remove(actor))));
    }

    /**
     * 获取监视者列表
     */
    public Set<ActorRef> getWatchers(ActorRef actor) {
        return watchedBy.getOrDefault(actor, Collections.emptySet());
    }

    /**
     * 获取被监视者列表
     */
    public Set<ActorRef> getWatched(ActorRef actor) {
        return watching.getOrDefault(actor, Collections.emptySet());
    }

    private void notifyWatcher(ActorRef watcher, ActorRef terminated) {
        watcher.tell(new Terminated(terminated), ActorRef.noSender());
    }

    /**
     * Actor终止通知消息
     */
    public static class Terminated {
        private final ActorRef actor;
        private final boolean existenceConfirmed;
        private final boolean addressTerminated;

        public Terminated(ActorRef actor) {
            this(actor, true, true);
        }

        public Terminated(ActorRef actor, boolean existenceConfirmed, boolean addressTerminated) {
            this.actor = actor;
            this.existenceConfirmed = existenceConfirmed;
            this.addressTerminated = addressTerminated;
        }

        public ActorRef getActor() {
            return actor;
        }

        public boolean isExistenceConfirmed() {
            return existenceConfirmed;
        }

        public boolean isAddressTerminated() {
            return addressTerminated;
        }
    }
}