package com.avolution.net.udp;

import com.avolution.net.MessagePacket;

public class UDPPacket extends MessagePacket {
    private int sequenceNumber; // 序列号
    private int acknowledgmentNumber; // 确认号

    public UDPPacket(int sequenceNumber, int acknowledgmentNumber, byte[] content) {
        super(8 + content.length, content); // 2个int类型的字段共8字节，加上包体内容长度
        this.sequenceNumber = sequenceNumber;
        this.acknowledgmentNumber = acknowledgmentNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAcknowledgmentNumber() {
        return acknowledgmentNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setAcknowledgmentNumber(int acknowledgmentNumber) {
        this.acknowledgmentNumber = acknowledgmentNumber;
    }
}
