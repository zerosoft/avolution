package com.avolution.net.udp;

import com.avolution.net.udp.codec.UDPPacketDecoder;
import com.avolution.net.udp.codec.UDPPacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import com.avolution.service.IService;

public class UDPNettyService implements IService {

    private final int port;
    private volatile Status status;

    public UDPNettyService(int port) {
        this.port = port;
        this.status = Status.STOPPED;
    }

    @Override
    public void start() {
        if (status == Status.RUNNING || status == Status.STARTING) {
            return;
        }
        status = Status.STARTING;
        new Thread(() -> {
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
                                SimpleUDPServerHandler handler = new SimpleUDPServerHandler();
                                ch.pipeline().addLast(handler);  // 业务处理器
                            }
                        });

                ChannelFuture f = b.bind(port).sync();
                System.out.println("UDPNettyService started and listening on port " + port);
                status = Status.RUNNING;

                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                status = Status.ERROR;
                Thread.currentThread().interrupt();
            } finally {
                group.shutdownGracefully();
                status = Status.STOPPED;
            }
        }).start();
    }

    @Override
    public void pause() {
        if (status == Status.RUNNING) {
            status = Status.PAUSED;
            // Implement pause logic if needed
        }
    }

    @Override
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            status = Status.STOPPING;
            // Implement stop logic if needed
            status = Status.STOPPED;
        }
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getStatusInfo() {
        return "UDPNettyService is currently " + status;
    }

    public static void main(String[] args) {
        int port = 8080;
        UDPNettyService service = new UDPNettyService(port);
        service.start();
    }
}
