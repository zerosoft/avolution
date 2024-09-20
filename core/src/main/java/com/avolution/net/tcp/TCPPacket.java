package com.avolution.net.tcp;

public class TCPPacket {
    private int length;        // 包体总长度
    private int protocolType;  // 协议类型
    private int encryptionType; // 加密类型
    private int protocolId;    // 协议ID
    private byte[] content;    // 包体内容

    public TCPPacket(int protocolType, int encryptionType, int protocolId, byte[] content) {
        this.length = 12 + content.length; // 4个int类型的字段共12字节，加上包体内容长度
        this.protocolType = protocolType;
        this.encryptionType = encryptionType;
        this.protocolId = protocolId;
        this.content = content;
    }

    public int getLength() {
        return length;
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

    public byte[] getContent() {
        return content;
    }
}
