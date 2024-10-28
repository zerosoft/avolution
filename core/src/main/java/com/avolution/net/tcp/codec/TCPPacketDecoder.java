package com.avolution.net.tcp.codec;

import com.avolution.net.tcp.TCPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

public class TCPPacketDecoder extends ByteToMessageDecoder {

    private static final String AES_KEY = "1234567890123456"; // Example AES key, should be securely managed

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 确保至少有16字节可以读取 (4个int + 至少一些内容)
        if (in.readableBytes() < 16) {
            return;
        }

        // 标记当前读位置以便重置
        in.markReaderIndex();

        // 读取长度字段 (总长度应该等于4个int + content长度)
        int length = in.readInt();

        // 如果包体不完整，则重置读位置并等待更多字节到达
        if (in.readableBytes() < length - 4) {
            in.resetReaderIndex();
            return;
        }

        // 读取协议类型
        int protocolType = in.readInt();
        // 读取加密类型
        int encryptionType = in.readInt();
        // 读取协议ID
        int protocolId = in.readInt();

        // 读取包体内容
        byte[] content = new byte[length - 12];  // 剩余字节是包体
        in.readBytes(content);

        // 解密包体内容
        content = decryptContent(content, encryptionType);

        // 构造TCPPacket并添加到out中
        TCPPacket packet = new TCPPacket(protocolType, encryptionType, protocolId, content);
        out.add(packet);
    }

    private byte[] decryptContent(byte[] content, int encryptionType) throws Exception {
        switch (encryptionType) {
            case 0:
                // No decryption
                return content;
            case 1:
                // Basic decryption (XOR decryption)
                return xorDecrypt(content);
            case 2:
                // Advanced decryption (AES decryption)
                return aesDecrypt(content);
            case 3:
                // Custom decryption (user-defined)
                return customDecrypt(content);
            default:
                throw new IllegalArgumentException("Unknown encryption type: " + encryptionType);
        }
    }

    private byte[] xorDecrypt(byte[] content) {
        byte[] decrypted = new byte[content.length];
        byte key = 0x5A; // Example XOR key
        for (int i = 0; i < content.length; i++) {
            decrypted[i] = (byte) (content[i] ^ key);
        }
        return decrypted;
    }

    private byte[] aesDecrypt(byte[] content) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(content);
    }

    private byte[] customDecrypt(byte[] content) {
        // Implement custom decryption logic here
        return content;
    }
}
