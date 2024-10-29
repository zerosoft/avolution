package com.avolution.net.tcp.codec;

import com.avolution.net.tcp.TCPPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class TCPPacketDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LogManager.getLogger(TCPPacketDecoder.class);

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

        // 构造TCPPacket并添加到out中
        TCPPacket packet = new TCPPacket(protocolType, encryptionType, protocolId, content);
        out.add(packet);

        // Add debug logs to trace the decoding process
        logger.debug("Decoded TCPPacket: protocolType={}, encryptionType={}, protocolId={}, content={}",
                protocolType, encryptionType, protocolId, new String(content));
    }
}
