package com.avolution.service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ServiceManager {
    private final Map<String, IService> services;

    private final ExecutorService executorService;

    private final ReentrantLock lock;

    private final int maxRetries;

    private final long initialRetryDelay;

    public ServiceManager() {
        this.services = new ConcurrentHashMap<>();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.lock = new ReentrantLock();
        this.maxRetries = 3;
        this.initialRetryDelay = 1000; // 1 second
    }

    public CompletableFuture<Void> schedule(Runnable task, Duration delay) {
        // 创建一个 CompletableFuture，用于调度任务
        return CompletableFuture.runAsync(() -> {
            try {
                // 等待指定的延迟时间
                Thread.sleep(delay.toMillis());
                // 执行任务
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 处理中断
            }
        }, executorService);
    }

    public void addService(String name, IService service) {
        lock.lock();
        try {
            services.put(name, service);
        } finally {
            lock.unlock();
        }
    }

    public void removeService(String name) {
        lock.lock();
        try {
            services.remove(name);
        } finally {
            lock.unlock();
        }
    }

    public IService getService(String name) {
        lock.lock();
        try {
            return services.get(name);
        } finally {
            lock.unlock();
        }
    }

    public void startAllServices() {
        lock.lock();
        try {
            for (String service : services.keySet()) {
                startService(service);
            }
        } finally {
            lock.unlock();
        }
    }

    public void pauseAllServices() {
        lock.lock();
        try {
            for (String service : services.keySet()) {
                pauseService(service);
            }
        } finally {
            lock.unlock();
        }
    }

    public void stopAllServices() {
        lock.lock();
        try {
            for (String service : services.keySet()) {
                stopService(service);
            }
        } finally {
            lock.unlock();
        }
    }

    public void restartAllServices() {
        lock.lock();
        try {
            for (String service : services.keySet()) {
                restartService(service);
            }
        } finally {
            lock.unlock();
        }
    }

    public Map<String, IService.Status> getAllServiceStatuses() {
        lock.lock();
        try {
            Map<String, IService.Status> statuses = new HashMap<>();
            for (Map.Entry<String, IService> entry : services.entrySet()) {
                statuses.put(entry.getKey(), entry.getValue().getStatus());
            }
            return statuses;
        } finally {
            lock.unlock();
        }
    }

    public boolean confirmAllServicesStarted() {
        lock.lock();
        try {
            for (IService service : services.values()) {
                if (!service.isRunning()) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void startService(String name) {
        executorService.submit(() -> {
            lock.lock();
            try {
                IService service = services.get(name);
                if (service != null) {
                    attemptStartService(service, 0);
                }
            } finally {
                lock.unlock();
            }
        });
    }

    private void attemptStartService(IService service, int attempt) {
        try {
            service.start();
            if (!service.isRunning() && attempt < maxRetries) {
                long delay = initialRetryDelay * (1 << attempt); // Exponential backoff
                schedule(() -> attemptStartService(service, attempt + 1),Duration.ofMillis(delay));
            } else if (!service.isRunning()) {
                System.err.println("Failed to start service after " + maxRetries + " attempts: " + service.getStatusInfo());
            }
        } catch (Exception e) {
            if (attempt < maxRetries) {
                long delay = initialRetryDelay * (1 << attempt); // Exponential backoff
                schedule(() -> attemptStartService(service, attempt + 1), Duration.ofMillis(delay));
            } else {
                System.err.println("Failed to start service after " + maxRetries + " attempts: " + service.getStatusInfo());
            }
        }
    }

    private void pauseService(String name) {
        executorService.submit(() -> {
            lock.lock();
            try {
                IService service = services.get(name);
                if (service != null) {
                    service.pause();
                }
            } finally {
                lock.unlock();
            }
        });
    }

    private void stopService(String name) {
        executorService.submit(() -> {
            lock.lock();
            try {
                IService service = services.get(name);
                if (service != null) {
                    service.stop();
                }
            } finally {
                lock.unlock();
            }
        });
    }

    private void restartService(String name) {
        executorService.submit(() -> {
            lock.lock();
            try {
                IService service = services.get(name);
                if (service != null) {
                    service.restart();
                }
            } finally {
                lock.unlock();
            }
        });
    }
}
