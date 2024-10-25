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

public class TCPClientService {

    private final String host;
    private final int port;
    private final RemoteActor remoteActor;

    public TCPClientService(String host, int port) {
        this.host = host;
        this.port = port;
        this.remoteActor = new RemoteActor(host, port);
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
        TCPPacket packet = new TCPPacket(1, 0, 1001, content.getBytes());
        // 发送给服务器
        f.channel().writeAndFlush(packet);

        // Use RemoteActor to send a message
        remoteActor.sendMessage(remoteActor, new Message(content));
    }

    public static void main(String[] args) throws InterruptedException {
        String host = "127.0.0.1";  // 服务器地址
        int port = 8080;  // 服务器端口
        new TCPClientService(host, port).start();
    }
}
