package com.avolution.actor.core;

/**
 *
 * Actor生命周期回调接口
 *
 */
public interface ActorLifecycleCallback {
    // Actor启动前回调
    void onPreStart();
    // Actor启动后回调
    void onPostStart();
    // Actor停止前回调
    void onPreStop();
    // Actor停止后回调
    void onPostStop();
    // Actor重启前回调
    void onPreRestart(Throwable reason);
    // Actor重启后回调
    void onPostRestart(Throwable reason);
}
