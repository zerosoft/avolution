package com.avolution.net.udp.codec;

import com.avolution.net.MessagePacket;
import com.avolution.net.udp.UDPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class UDPPacketEncoder extends MessageToByteEncoder<MessagePacket> {

    private static final Logger logger = LogManager.getLogger(UDPPacketEncoder.class);

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

        // Add debug logs to trace the encoding process
        logger.debug("Encoded UDPPacket: sequenceNumber={}, acknowledgmentNumber={}, content={}",
                ((UDPPacket) msg).getSequenceNumber(), ((UDPPacket) msg).getAcknowledgmentNumber(), new String(msg.getContent()));
    }
}
