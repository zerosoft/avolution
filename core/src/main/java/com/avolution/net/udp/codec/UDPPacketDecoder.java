package com.avolution.net.udp.codec;

import com.avolution.net.udp.UDPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

public class UDPPacketDecoder extends ByteToMessageDecoder {

    private static final String AES_KEY = "1234567890123456"; // Example AES key, should be securely managed

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Ensure at least 8 bytes are readable (2 ints + at least some content)
        if (in.readableBytes() < 8) {
            return;
        }

        // Mark the current read position to reset later
        in.markReaderIndex();

        // Read the length field (total length should be 2 ints + content length)
        int length = in.readInt();

        // If the packet body is incomplete, reset the read position and wait for more bytes
        if (in.readableBytes() < length - 4) {
            in.resetReaderIndex();
            return;
        }

        // Read the sequence number
        int sequenceNumber = in.readInt();
        // Read the acknowledgment number
        int acknowledgmentNumber = in.readInt();

        // Read the packet body content
        byte[] content = new byte[length - 8];  // Remaining bytes are the packet body
        in.readBytes(content);

        // Decrypt the packet body content
        content = decryptContent(content);

        // Construct UDPPacket and add to out
        UDPPacket packet = new UDPPacket(sequenceNumber, acknowledgmentNumber, content);
        out.add(packet);
    }

    private byte[] decryptContent(byte[] content) throws Exception {
        // Implement decryption logic here (e.g., AES decryption)
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(content);
    }
}
