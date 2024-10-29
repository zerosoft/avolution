package com.avolution.net.tcp.codec;

import com.avolution.net.MessagePacket;
import com.avolution.net.tcp.TCPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class TCPPacketEncoder extends MessageToByteEncoder<MessagePacket> {

    private static final Logger logger = LogManager.getLogger(TCPPacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePacket msg, ByteBuf out) throws Exception {
        // 写入总长度
        out.writeInt(msg.getLength());
        // 写入协议类型
        out.writeInt(((TCPPacket) msg).getProtocolType());
        // 写入加密类型
        out.writeInt(((TCPPacket) msg).getEncryptionType());
        // 写入协议ID
        out.writeInt(((TCPPacket) msg).getProtocolId());
        // 写入包体内容
        out.writeBytes(msg.getContent());

        // Add debug logs to trace the encoding process
        logger.debug("Encoded TCPPacket: protocolType={}, encryptionType={}, protocolId={}, content={}",
                ((TCPPacket) msg).getProtocolType(), ((TCPPacket) msg).getEncryptionType(), ((TCPPacket) msg).getProtocolId(), new String(msg.getContent()));
    }
}
