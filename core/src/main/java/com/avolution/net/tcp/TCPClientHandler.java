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
    }

    private void handleBasicEncryption(TCPPacket packet) {
        // Implement basic encryption handling logic here
    }

    private void handleAdvancedEncryption(TCPPacket packet) {
        // Implement advanced encryption handling logic here
    }

    private void handleCustomEncryption(TCPPacket packet) {
        // Implement custom encryption handling logic here
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
