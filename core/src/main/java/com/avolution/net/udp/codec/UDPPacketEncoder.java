package com.avolution.net.udp.codec;

import com.avolution.net.MessagePacket;
import com.avolution.net.udp.UDPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class UDPPacketEncoder extends MessageToByteEncoder<MessagePacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePacket msg, ByteBuf out) throws Exception {
        // Write the total length
        out.writeInt(msg.getLength());
        // Write the sequence number
        out.writeInt(((UDPPacket) msg).getSequenceNumber());
        // Write the acknowledgment number
        out.writeInt(((UDPPacket) msg).getAcknowledgmentNumber());
        // Write the packet content
        out.writeBytes(msg.getContent());
    }
}
