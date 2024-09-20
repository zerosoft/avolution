package com.avolution.net.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SimpleServerHandler extends SimpleChannelInboundHandler<TCPPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TCPPacket packet) {
        System.out.println("Received packet:");
        System.out.println("Protocol Type: " + packet.getProtocolType());
        System.out.println("Encryption Type: " + packet.getEncryptionType());
        System.out.println("Protocol ID: " + packet.getProtocolId());
        System.out.println("Content: " + new String(packet.getContent()));

        // 回复一个新TCPPacket作为响应
        String responseContent = "Response to Protocol ID " + packet.getProtocolId();
        TCPPacket responsePacket = new TCPPacket(packet.getProtocolType(), packet.getEncryptionType(),
                packet.getProtocolId(), responseContent.getBytes());
        ctx.writeAndFlush(responsePacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
