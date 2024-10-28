package com.avolution.actor;

import com.avolution.net.tcp.TCPClientService;
import com.avolution.net.tcp.TCPPacket;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;

public class RemoteActor extends AbstractActor {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final TCPClientService tcpClientService;
    private final BlockingQueue<Message> messageQueue;
    private final ExecutorService executorService;

    public RemoteActor(String host, int port) {
        super();
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
            byte[] compressedMessage = compressMessage(message.getContent().getBytes());
            tcpClientService.send(compressedMessage);
        } catch (IOException e) {
            e.printStackTrace();
            retryConnection();
        }
    }

    public byte[] compressMessage(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
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
