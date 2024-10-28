package com.avolution.net.udp;

import com.avolution.net.udp.codec.UDPPacketDecoder;
import com.avolution.net.udp.codec.UDPPacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UDPClientService {

    private final String host;
    private final int port;
    private final ConcurrentLinkedQueue<ChannelFuture> connectionPool;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Integer, UDPPacket> sentPackets;
    private final ConcurrentHashMap<Integer, Long> sentTimestamps;
    private final int windowSize;
    private int sequenceNumber;
    private int acknowledgmentNumber;

    public UDPClientService(String host, int port) {
        this.host = host;
        this.port = port;
        this.connectionPool = new ConcurrentLinkedQueue<>();
        this.executorService = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.sentPackets = new ConcurrentHashMap<>();
        this.sentTimestamps = new ConcurrentHashMap<>();
        this.windowSize = 10;
        this.sequenceNumber = 0;
        this.acknowledgmentNumber = 0;
    }

    public void start() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        public void initChannel(DatagramChannel ch) {
                            ch.pipeline().addLast(new UDPPacketDecoder());  // 自定义解码器
                            ch.pipeline().addLast(new UDPPacketEncoder());  // 自定义编码器
                            ch.pipeline().addLast(new UDPClientHandler());  // 客户端处理器
                        }
                    });

            // 连接到服务器
            ChannelFuture f = b.connect(host, port).sync();
            System.out.println("Connected to server: " + host + ":" + port);

            // 发送一个初始包到服务器
            sendInitialPacket(f);

            // 启动重传机制
            startRetransmission();

            // 等待连接关闭
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private void sendInitialPacket(ChannelFuture f) {
        // 创建初始的UDPPacket
        String content = "Hello, Server!";
        send(content.getBytes());
    }

    public void send(String content) {
        send(content.getBytes());
    }

    public void send(byte[] content) {
        executorService.submit(() -> {
            ChannelFuture f = getConnection();
            if (f != null) {
                UDPPacket packet = new UDPPacket(sequenceNumber++, acknowledgmentNumber, content);
                sentPackets.put(packet.getSequenceNumber(), packet);
                sentTimestamps.put(packet.getSequenceNumber(), System.currentTimeMillis());
                f.channel().writeAndFlush(packet);
            }
        });
    }

    private ChannelFuture getConnection() {
        ChannelFuture f = connectionPool.poll();
        if (f == null || !f.channel().isActive()) {
            f = createNewConnection();
        }
        return f;
    }

    private ChannelFuture createNewConnection() {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    public void initChannel(DatagramChannel ch) {
                        ch.pipeline().addLast(new UDPPacketDecoder());  // 自定义解码器
                        ch.pipeline().addLast(new UDPPacketEncoder());  // 自定义编码器
                        ch.pipeline().addLast(new UDPClientHandler());  // 客户端处理器
                    }
                });

        try {
            ChannelFuture f = b.connect(host, port).sync();
            f.channel().closeFuture().addListener(future -> {
                group.shutdownGracefully();
                connectionPool.remove(f);
            });
            connectionPool.add(f);
            return f;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void startRetransmission() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            for (int seqNum : sentPackets.keySet()) {
                if (currentTime - sentTimestamps.get(seqNum) > 1000) { // 1 second timeout
                    UDPPacket packet = sentPackets.get(seqNum);
                    send(packet.getContent());
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void acknowledgePacket(int sequenceNumber) {
        sentPackets.remove(sequenceNumber);
        sentTimestamps.remove(sequenceNumber);
    }

    public static void main(String[] args) throws InterruptedException {
        String host = "127.0.0.1";  // 服务器地址
        int port = 8080;  // 服务器端口
        UDPClientService clientService = new UDPClientService(host, port);
        clientService.start();

        clientService.send("Hello");
    }
}
