package com.avolution.actor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RemoteActorTest {

    private RemoteActor remoteActor;

    @BeforeEach
    void setUp() {
        remoteActor = new RemoteActor("localhost", 8080);
    }

    @Test
    void testReceiveMessage() {
        Message message = new Message("Hello");
        remoteActor.receiveMessage(message);
        // Add assertions to verify message handling
    }

    @Test
    void testSendMessage() {
        RemoteActor recipient = new RemoteActor("localhost", 8081);
        Message message = new Message("Hello");
        remoteActor.sendMessage(recipient, message);
        // Add assertions to verify message sending
    }

    @Test
    void testRestart() {
        remoteActor.restart();
        // Add assertions to verify restart functionality
    }

    @Test
    void testStop() {
        remoteActor.stop();
        // Add assertions to verify stop functionality
    }

    @Test
    void testResume() {
        remoteActor.resume();
        // Add assertions to verify resume functionality
    }

    @Test
    void testIntegrationWithAbstractActor() {
        assertTrue(remoteActor instanceof AbstractActor);
    }

    @Test
    void testSendStringMessage() {
        RemoteActor recipient = new RemoteActor("localhost", 8081);
        Message message = new Message("Hello");
        remoteActor.sendMessage(recipient, message);
        // Add assertions to verify string message sending
    }

    @Test
    void testSendByteArrayMessage() {
        RemoteActor recipient = new RemoteActor("localhost", 8081);
        byte[] messageContent = "Hello".getBytes();
        Message message = new Message(new String(messageContent));
        remoteActor.sendMessage(recipient, message);
        // Add assertions to verify byte array message sending
    }

    @Test
    void testMessageCompression() {
        String originalMessage = "This is a test message for compression";
        byte[] compressedMessage = null;
//        try {
//            compressedMessage = remoteActor.compressMessage(originalMessage.getBytes());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        assertNotNull(compressedMessage);
        assertTrue(compressedMessage.length < originalMessage.getBytes().length);
    }

    @Test
    void testBatchingMessages() {
        RemoteActor recipient = new RemoteActor("localhost", 8081);
        for (int i = 0; i < 10; i++) {
            Message message = new Message("Message " + i);
            remoteActor.sendMessage(recipient, message);
        }
        // Add assertions to verify message batching
    }
}
