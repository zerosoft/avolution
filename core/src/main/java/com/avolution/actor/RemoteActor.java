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
    private final TCPClientService tcpClientService;
    private final BlockingQueue<Message> messageQueue;
    private final ExecutorService executorService;

    public RemoteActor(String id, String host, int port) {
        super(id);
        this.host = host;
        this.port = port;
        this.tcpClientService = new TCPClientService(host, port);
        this.messageQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        connect();
        this.executorService.submit(this::processMessages);
    }

    private void connect() {
        try {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
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
                TCPPacket packet = (TCPPacket) in.readObject();
                Message message = new Message(new String(packet.getContent()));
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
            TCPPacket packet = new TCPPacket(1, 0, 1, message.getContent().getBytes());
            tcpClientService.send(packet.getContent());
        } catch (Exception e) {
            e.printStackTrace();
            retryConnection();
        }
    }

    private void processMessages() {
        try {
            while (true) {
                Message message = messageQueue.take();
                handleMessage(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
