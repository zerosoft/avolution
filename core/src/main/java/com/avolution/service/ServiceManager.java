package com.avolution.service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ServiceManager {
    private final Map<String, IService> services;
    private final Map<String, List<String>> dependencies;
    private final ExecutorService executorService;
    private final ReentrantLock lock;

    public ServiceManager() {
        this.services = new ConcurrentHashMap<>();
        this.dependencies = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.lock = new ReentrantLock();
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
            dependencies.remove(name);
            dependencies.values().forEach(deps -> deps.remove(name));
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

    public void addDependency(String service, String dependency) {
        lock.lock();
        try {
            dependencies.computeIfAbsent(service, k -> new ArrayList<>()).add(dependency);
        } finally {
            lock.unlock();
        }
    }

    public void removeDependency(String service, String dependency) {
        lock.lock();
        try {
            List<String> deps = dependencies.get(service);
            if (deps != null) {
                deps.remove(dependency);
            }
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

    public Map<String, String> getAllServiceStatuses() {
        lock.lock();
        try {
            Map<String, String> statuses = new HashMap<>();
            for (Map.Entry<String, IService> entry : services.entrySet()) {
                statuses.put(entry.getKey(), entry.getValue().getStatus());
            }
            return statuses;
        } finally {
            lock.unlock();
        }
    }

    private void startService(String name) {
        executorService.submit(() -> {
            lock.lock();
            try {
                List<String> deps = dependencies.get(name);
                if (deps != null) {
                    for (String dep : deps) {
                        startService(dep);
                    }
                }
                IService service = services.get(name);
                if (service != null) {
                    service.start();
                }
            } finally {
                lock.unlock();
            }
        });
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
                List<String> deps = dependencies.get(name);
                if (deps != null) {
                    for (String dep : deps) {
                        stopService(dep);
                    }
                }
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
