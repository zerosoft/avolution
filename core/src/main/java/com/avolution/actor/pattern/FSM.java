package com.avolution.actor.pattern;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 有限状态机实现
 */
public class FSM {
    private final StatefulActor actor;
    private volatile Object currentState;
    private Object currentData;
    private final Map<Object, List<StateHandler>> stateHandlers;
    private final List<TransitionListener> transitionListeners;
    private final Timer timeoutTimer;
    private final Object timerLock = new Object();

    public FSM(StatefulActor actor) {
        this.actor = actor;
        this.stateHandlers = new ConcurrentHashMap<>();
        this.transitionListeners = new ArrayList<>();
        this.timeoutTimer = new Timer("FSM-Timer", true);
    }

    /**
     * 定义状态处理
     */
    public void when(Object state, Predicate<Object> predicate, Function<Object, Object> handler) {
        stateHandlers.computeIfAbsent(state, k -> new ArrayList<>())
                    .add(new StateHandler(predicate, handler));
    }

    /**
     * 定义带超时的状态处理
     */
    public void whenWithTimeout(Object state, 
                              Predicate<Object> predicate, 
                              Function<Object, Object> handler,
                              long timeout,
                              Object timeoutState) {
        when(state, predicate, handler);
        setTimeout(state, timeout, timeoutState);
    }

    /**
     * 设置状态
     */
    public void setState(Object newState) {
        Object oldState = this.currentState;
        this.currentState = newState;
        
        // 取消之前的超时定时器
        synchronized (timerLock) {
            timeoutTimer.purge();
        }
        
        // 通知状态转换
        notifyTransition(new Transition(oldState, newState, currentData));
    }

    /**
     * 设置状态和数据
     */
    public void setState(Object newState, Object newData) {
        Object oldState = this.currentState;
        this.currentState = newState;
        this.currentData = newData;
        
        notifyTransition(new Transition(oldState, newState, newData));
    }

    /**
     * 获取当前状态
     */
    public Object getState() {
        return currentState;
    }

    /**
     * 获取当前数据
     */
    public Object getData() {
        return currentData;
    }

    /**
     * 处理消息
     */
    public boolean handleMessage(Object message) {
        List<StateHandler> handlers = stateHandlers.get(currentState);
        if (handlers != null) {
            for (StateHandler handler : handlers) {
                if (handler.predicate.test(message)) {
                    Object result = handler.handler.apply(message);
                    if (result != null) {
                        currentData = result;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 添加转换监听器
     */
    public void addTransitionListener(TransitionListener listener) {
        transitionListeners.add(listener);
    }

    /**
     * 设置状态超时
     */
    public void setTimeout(Object state, long timeout, Object timeoutState) {
        synchronized (timerLock) {
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (currentState.equals(state)) {
                        setState(timeoutState);
                    }
                }
            }, timeout);
        }
    }

    /**
     * 状态处理器
     */
    private static class StateHandler {
        final Predicate<Object> predicate;
        final Function<Object, Object> handler;

        StateHandler(Predicate<Object> predicate, Function<Object, Object> handler) {
            this.predicate = predicate;
            this.handler = handler;
        }
    }

    /**
     * 状态转换
     */
    public static class Transition {
        private final Object fromState;
        private final Object toState;
        private final Object data;

        public Transition(Object fromState, Object toState, Object data) {
            this.fromState = fromState;
            this.toState = toState;
            this.data = data;
        }

        public Object getFromState() { return fromState; }
        public Object getToState() { return toState; }
        public Object getData() { return data; }
    }

    /**
     * 转换监听器接口
     */
    public interface TransitionListener {
        void onTransition(Transition transition);
    }

    /**
     * 状态Actor接口
     */
    public interface StatefulActor {
        void onTransition(Transition transition);
    }

    /**
     * 通知状态转换
     */
    private void notifyTransition(Transition transition) {
        actor.onTransition(transition);
        for (TransitionListener listener : transitionListeners) {
            listener.onTransition(transition);
        }
    }
} 