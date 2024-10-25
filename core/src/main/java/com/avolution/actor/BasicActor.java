package com.avolution.actor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicActor extends AbstractActor {

    public BasicActor() {
        super();
    }

    @Override
    protected void handleMessage(Message message) {
        // Custom message handling logic
        System.out.println("Processing message: " + message.getContent());
        // Add more custom handling logic here
    }
}
