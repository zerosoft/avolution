package com.avolution.net.udp.codec;

import com.avolution.net.udp.UDPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class UDPPacketDecoder extends ByteToMessageDecoder {

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

        // Construct UDPPacket and add to out
        UDPPacket packet = new UDPPacket(sequenceNumber, acknowledgmentNumber, content);
        out.add(packet);
    }
}
