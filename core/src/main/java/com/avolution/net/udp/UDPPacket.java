package com.avolution.net.udp;

public class UDPPacket {
    private int length;        // 包体总长度
    private int sequenceNumber; // 序列号
    private int acknowledgmentNumber; // 确认号
    private byte[] content;    // 包体内容

    public UDPPacket(int sequenceNumber, int acknowledgmentNumber, byte[] content) {
        this.length = 8 + content.length; // 2个int类型的字段共8字节，加上包体内容长度
        this.sequenceNumber = sequenceNumber;
        this.acknowledgmentNumber = acknowledgmentNumber;
        this.content = content;
    }

    public int getLength() {
        return length;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAcknowledgmentNumber() {
        return acknowledgmentNumber;
    }

    public byte[] getContent() {
        return content;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setAcknowledgmentNumber(int acknowledgmentNumber) {
        this.acknowledgmentNumber = acknowledgmentNumber;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.length = 8 + content.length;
    }
}
