package com.avolution.net.tcp;

import com.avolution.actor.BasicActor;
import com.avolution.actor.Message;
import com.avolution.actor.Supervisor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SimpleServerHandler extends SimpleChannelInboundHandler<TCPPacket> {

    private final BasicActor actor;
    private final Supervisor supervisor;

    public SimpleServerHandler(Supervisor supervisor) {
        this.actor = new BasicActor();
        this.supervisor = supervisor;
        this.supervisor.addActor(actor);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TCPPacket packet) {
        System.out.println("Received packet:");
        System.out.println("Protocol Type: " + packet.getProtocolType());
        System.out.println("Encryption Type: " + packet.getEncryptionType());
        System.out.println("Protocol ID: " + packet.getProtocolId());
        System.out.println("Content: " + new String(packet.getContent()));

        // Use BasicActor to handle the packet
        actor.receiveMessage(new Message(new String(packet.getContent())));

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
