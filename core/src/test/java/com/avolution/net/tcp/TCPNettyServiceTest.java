package com.avolution.net.tcp;

import com.avolution.service.IService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TCPNettyServiceTest {

    private TCPNettyService service;

    @BeforeEach
    void setUp() {
        service = new TCPNettyService(8080);
    }

    @Test
    void testStart() {
        service.start();
        assertTrue(service.isRunning());
    }

    @Test
    void testPause() {
        service.start();
        service.pause();
        assertEquals(IService.Status.PAUSED, service.getStatus());
    }

    @Test
    void testStop() {
        service.start();
        service.stop();
        assertEquals(IService.Status.STOPPED, service.getStatus());
    }

    @Test
    void testRestart() {
        service.start();
        service.restart();
        assertTrue(service.isRunning());
    }

    @Test
    void testIsRunning() {
        service.start();
        assertTrue(service.isRunning());
        service.stop();
        assertFalse(service.isRunning());
    }

    @Test
    void testGetStatus() {
        assertEquals(IService.Status.STOPPED, service.getStatus());
        service.start();
        assertEquals(IService.Status.RUNNING, service.getStatus());
    }

    @Test
    void testGetStatusInfo() {
        assertEquals("TCPNettyService is currently STOPPED", service.getStatusInfo());
        service.start();
        assertEquals("TCPNettyService is currently RUNNING", service.getStatusInfo());
    }
}
