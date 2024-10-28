package com.avolution.service;

public interface IService {
    void start();
    void pause();
    void stop();
    void restart();
    String getStatus();
}
