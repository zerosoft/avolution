package com.avolution.net.udp;

import com.avolution.net.MessagePacket;

public class UDPPacket extends MessagePacket {
    private int sequenceNumber; // 序列号
    private int acknowledgmentNumber; // 确认号
    private int encryptionType; // 加密类型

    public UDPPacket(int sequenceNumber, int acknowledgmentNumber, byte[] content) {
        super(8 + content.length, content); // 2个int类型的字段共8字节，加上包体内容长度
        this.sequenceNumber = sequenceNumber;
        this.acknowledgmentNumber = acknowledgmentNumber;
        this.encryptionType = 0; // 默认加密类型
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAcknowledgmentNumber() {
        return acknowledgmentNumber;
    }

    public int getEncryptionType() {
        return encryptionType;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setAcknowledgmentNumber(int acknowledgmentNumber) {
        this.acknowledgmentNumber = acknowledgmentNumber;
    }

    public void setEncryptionType(int encryptionType) {
        this.encryptionType = encryptionType;
    }
}
