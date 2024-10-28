package com.avolution.service;

/**
 * Interface for a service that can be started, paused, stopped, and restarted.
 */
public interface IService {

    enum Status{
        STARTING,
        RUNNING,
        PAUSED,
        STOPPING,
        STOPPED,
        ERROR
    }

    /**
     * Start
     */
    void start();

    /**
     * Pause the service.
     */
    void pause();

    /**
     * Stop the service.
     */
    void stop();

    /**
     * Restart the service.
     */
    void restart();

    boolean isRunning();

    Status getStatus();

    String getStatusInfo();
}
