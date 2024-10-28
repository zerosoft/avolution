package com.avolution.net.tcp.codec;

import com.avolution.net.tcp.TCPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class TCPPacketEncoder extends MessageToByteEncoder<TCPPacket> {

    private static final String AES_KEY = "1234567890123456"; // Example AES key, should be securely managed

    @Override
    protected void encode(ChannelHandlerContext ctx, TCPPacket msg, ByteBuf out) throws Exception {
        // 写入总长度
        out.writeInt(msg.getLength());
        // 写入协议类型
        out.writeInt(msg.getProtocolType());
        // 写入加密类型
        out.writeInt(msg.getEncryptionType());
        // 写入协议ID
        out.writeInt(msg.getProtocolId());
        // 加密包体内容
        byte[] encryptedContent = encryptContent(msg.getContent(), msg.getEncryptionType());
        // 写入包体内容
        out.writeBytes(encryptedContent);
    }

    private byte[] encryptContent(byte[] content, int encryptionType) throws Exception {
        switch (encryptionType) {
            case 0:
                // No encryption
                return content;
            case 1:
                // Basic encryption (XOR encryption)
                return xorEncrypt(content);
            case 2:
                // Advanced encryption (AES encryption)
                return aesEncrypt(content);
            case 3:
                // Custom encryption (user-defined)
                return customEncrypt(content);
            default:
                throw new IllegalArgumentException("Unknown encryption type: " + encryptionType);
        }
    }

    private byte[] xorEncrypt(byte[] content) {
        byte[] encrypted = new byte[content.length];
        byte key = 0x5A; // Example XOR key
        for (int i = 0; i < content.length; i++) {
            encrypted[i] = (byte) (content[i] ^ key);
        }
        return encrypted;
    }

    private byte[] aesEncrypt(byte[] content) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(content);
    }

    private byte[] customEncrypt(byte[] content) {
        // Implement custom encryption logic here
        return content;
    }
}
