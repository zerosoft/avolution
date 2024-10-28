package com.avolution.net.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SimpleUDPServerHandler extends SimpleChannelInboundHandler<UDPPacket> {

    public SimpleUDPServerHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UDPPacket packet) {
        System.out.println("Received packet:");
        System.out.println("Sequence Number: " + packet.getSequenceNumber());
        System.out.println("Acknowledgment Number: " + packet.getAcknowledgmentNumber());
        System.out.println("Content: " + new String(packet.getContent()));

        // Handle different encryption types
        switch (packet.getEncryptionType()) {
            case 0:
                // No encryption handling needed
                break;
            case 1:
                // Handle basic encryption (e.g., XOR encryption)
                handleBasicEncryption(packet);
                break;
            case 2:
                // Handle advanced encryption (e.g., AES encryption)
                handleAdvancedEncryption(packet);
                break;
            case 3:
                // Handle custom encryption (user-defined)
                handleCustomEncryption(packet);
                break;
            default:
                throw new IllegalArgumentException("Unknown encryption type: " + packet.getEncryptionType());
        }

        // Output the message content
        System.out.println("Message Content: " + new String(packet.getContent()));

        // Reply with a new UDPPacket as a response
        String responseContent = "Response to Sequence Number " + packet.getSequenceNumber();
        UDPPacket responsePacket = new UDPPacket(packet.getSequenceNumber(), packet.getAcknowledgmentNumber(), responseContent.getBytes());
        ctx.writeAndFlush(responsePacket);
    }

    private void handleBasicEncryption(UDPPacket packet) {
        // Implement basic encryption handling logic here
    }

    private void handleAdvancedEncryption(UDPPacket packet) {
        // Implement advanced encryption handling logic here
    }

    private void handleCustomEncryption(UDPPacket packet) {
        // Implement custom encryption handling logic here
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
