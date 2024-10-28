package com.avolution.net.tcp;

import com.avolution.actor.RemoteActor;
import com.avolution.actor.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TCPClientService {

    private final String host;
    private final int port;
    private final RemoteActor remoteActor;
    private final ConcurrentLinkedQueue<ChannelFuture> connectionPool;
    private final ExecutorService executorService;

    public TCPClientService(String host, int port) {
        this.host = host;
        this.port = port;
        this.remoteActor = new RemoteActor(host, port);
        this.connectionPool = new ConcurrentLinkedQueue<>();
        this.executorService = Executors.newCachedThreadPool();
    }

    public void start() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TCPPacketDecoder());  // 自定义解码器
                            ch.pipeline().addLast(new TCPPacketEncoder());  // 自定义编码器
                            ch.pipeline().addLast(new TCPClientHandler());  // 客户端处理器
                        }
                    });

            // 连接到服务器
            ChannelFuture f = b.connect(host, port).sync();
            System.out.println("Connected to server: " + host + ":" + port);

            // 发送一个初始包到服务器
            sendInitialPacket(f);

            // 等待连接关闭
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private void sendInitialPacket(ChannelFuture f) {
        // 创建初始的TCPPacket
        String content = "Hello, Server!";
        send(content.getBytes());

        // Use RemoteActor to send a message
        remoteActor.sendMessage(remoteActor, new Message(content));
    }

    public void send(String content) {
        send(content.getBytes());
    }

    public void send(byte[] content) {
        executorService.submit(() -> {
            ChannelFuture f = getConnection();
            if (f != null) {
                TCPPacket packet = new TCPPacket(1, 0, 1001, content);
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
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TCPPacketDecoder());  // 自定义解码器
                        ch.pipeline().addLast(new TCPPacketEncoder());  // 自定义编码器
                        ch.pipeline().addLast(new TCPClientHandler());  // 客户端处理器
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

    public static void main(String[] args) throws InterruptedException {
        String host = "127.0.0.1";  // 服务器地址
        int port = 8080;  // 服务器端口
        new TCPClientService(host, port).start();
    }
}
