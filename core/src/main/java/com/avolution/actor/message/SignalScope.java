package com.avolution.actor.message;

public enum SignalScope {
    SINGLE,     // 仅当前Actor
    CHILDREN,   // 当前Actor及其子Actor
    SUBTREE,    // 当前Actor及其所有后代
    BROADCAST   // 系统广播
}