package com.avolution.net.udp.codec;

import com.avolution.net.udp.UDPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class UDPPacketEncoder extends MessageToByteEncoder<UDPPacket> {

    private static final String AES_KEY = "1234567890123456"; // Example AES key, should be securely managed

    @Override
    protected void encode(ChannelHandlerContext ctx, UDPPacket msg, ByteBuf out) throws Exception {
        // Write the total length
        out.writeInt(msg.getLength());
        // Write the sequence number
        out.writeInt(msg.getSequenceNumber());
        // Write the acknowledgment number
        out.writeInt(msg.getAcknowledgmentNumber());
        // Encrypt the packet content
        byte[] encryptedContent = encryptContent(msg.getContent());
        // Write the packet content
        out.writeBytes(encryptedContent);
    }

    private byte[] encryptContent(byte[] content) throws Exception {
        // Implement encryption logic here (e.g., AES encryption)
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(content);
    }
}
