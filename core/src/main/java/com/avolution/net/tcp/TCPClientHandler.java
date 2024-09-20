package com.avolution.net.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TCPClientHandler extends SimpleChannelInboundHandler<TCPPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TCPPacket packet) {
        System.out.println("Received packet from server:");
        System.out.println("Protocol Type: " + packet.getProtocolType());
        System.out.println("Encryption Type: " + packet.getEncryptionType());
        System.out.println("Protocol ID: " + packet.getProtocolId());
        System.out.println("Content: " + new String(packet.getContent()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
