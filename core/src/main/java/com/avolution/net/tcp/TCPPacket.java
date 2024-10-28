package com.avolution.net.tcp;

import com.avolution.net.MessagePacket;

public class TCPPacket extends MessagePacket {
    private int protocolType;  // 协议类型
    private int encryptionType; // 加密类型
    private int protocolId;    // 协议ID

    public TCPPacket(int protocolType, int encryptionType, int protocolId, byte[] content) {
        super(12 + content.length, content); // 4个int类型的字段共12字节，加上包体内容长度
        this.protocolType = protocolType;
        this.encryptionType = encryptionType;
        this.protocolId = protocolId;
    }

    public int getProtocolType() {
        return protocolType;
    }

    public int getEncryptionType() {
        return encryptionType;
    }

    public int getProtocolId() {
        return protocolId;
    }
}
