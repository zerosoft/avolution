package com.avolution.actor;

public interface Actor {
    void receiveMessage(Message message);
    void sendMessage(Actor recipient, Message message);
    String getId();
}
