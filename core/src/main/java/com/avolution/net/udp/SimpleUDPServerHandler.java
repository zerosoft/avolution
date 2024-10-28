package com.avolution.net.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SimpleUDPServerHandler extends SimpleChannelInboundHandler<UDPPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UDPPacket packet) {
        System.out.println("Received packet:");
        System.out.println("Sequence Number: " + packet.getSequenceNumber());
        System.out.println("Acknowledgment Number: " + packet.getAcknowledgmentNumber());
        System.out.println("Content: " + new String(packet.getContent()));

        // Handle the packet content and send a response
        String responseContent = "Response to Sequence Number " + packet.getSequenceNumber();
        UDPPacket responsePacket = new UDPPacket(packet.getSequenceNumber(), packet.getAcknowledgmentNumber(), responseContent.getBytes());
        ctx.writeAndFlush(responsePacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
