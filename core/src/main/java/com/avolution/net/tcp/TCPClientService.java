package com.avolution.net.tcp;

import com.avolution.net.MessagePacket;
import com.avolution.net.tcp.codec.TCPPacketDecoder;
import com.avolution.net.tcp.codec.TCPPacketEncoder;
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

public class TCPClientService {

    private final String host;
    private final int port;
    private final ConcurrentLinkedQueue<ChannelFuture> connectionPool;
    private final ExecutorService executorService;

    public TCPClientService(String host, int port) {
        this.host = host;
        this.port = port;
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
        // 创建初始的MessagePacket
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
                MessagePacket packet = new MessagePacket(12 + content.length, content);
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
        TCPClientService clientService = new TCPClientService(host, port);
        clientService.start();

        clientService.send("Hello");
    }
}
