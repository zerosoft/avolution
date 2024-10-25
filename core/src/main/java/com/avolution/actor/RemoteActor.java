package com.avolution.actor;

import com.avolution.net.tcp.TCPClientService;
import com.avolution.net.tcp.TCPPacket;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteActor extends AbstractActor {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public RemoteActor(String host, int port) {
        super();
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() {
        try {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.executorService.submit(this::receiveMessages);
        } catch (IOException e) {
            e.printStackTrace();
            retryConnection();
        }
    }

    private void retryConnection() {
        try {
            Thread.sleep(5000);
            connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                Message message = (Message) in.readObject();
                receiveMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            retryConnection();
        }
    }

    @Override
    protected void handleMessage(Message message) {
        // Process the message
        System.out.println("Processing message: " + message.getContent());
    }

    @Override
    public void sendMessage(Actor recipient, Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            retryConnection();
        }
    }
}
