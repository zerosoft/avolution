package com.avolution.actor;

import com.avolution.net.tcp.TCPClientService;
import com.avolution.net.tcp.TCPPacket;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteActor implements Actor {
    private final String host;
    private final int port;
    private final BlockingQueue<Message> messageQueue;
    private final ExecutorService executorService;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public RemoteActor(String host, int port) {
        this.host = host;
        this.port = port;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this::processMessages);
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
                messageQueue.offer(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            retryConnection();
        }
    }

    @Override
    public void receiveMessage(Message message) {
        messageQueue.offer(message);
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

    private void processMessages() {
        try {
            while (true) {
                Message message = messageQueue.take();
                // Process the message
                System.out.println("Processing message: " + message.getContent());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
