package com.avolution.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceManagerTest {

    private ServiceManager serviceManager;
    private IService mockService1;
    private IService mockService2;

    @BeforeEach
    void setUp() {
        serviceManager = new ServiceManager();
        mockService1 = mock(IService.class);
        mockService2 = mock(IService.class);
        serviceManager.addService("Service1", mockService1);
        serviceManager.addService("Service2", mockService2);
    }

    @Test
    void testConfirmAllServicesStarted() {
        when(mockService1.isRunning()).thenReturn(true);
        when(mockService2.isRunning()).thenReturn(true);

        assertTrue(serviceManager.confirmAllServicesStarted());

        when(mockService2.isRunning()).thenReturn(false);

        assertFalse(serviceManager.confirmAllServicesStarted());
    }

    @Test
    void testRetryMechanism() throws InterruptedException {
        when(mockService1.isRunning()).thenReturn(false);
        doThrow(new RuntimeException("Service start failed")).when(mockService1).start();

        serviceManager.startAllServices();

        Thread.sleep(5000); // Wait for retries to complete

        verify(mockService1, times(4)).start(); // 1 initial attempt + 3 retries
    }
}
