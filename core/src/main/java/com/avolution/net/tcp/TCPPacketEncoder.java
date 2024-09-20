package com.avolution.net.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class TCPPacketEncoder extends MessageToByteEncoder<TCPPacket> {

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
        // 写入包体内容
        out.writeBytes(msg.getContent());
    }
}
