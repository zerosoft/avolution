//package com.avolution.actor.routing;
//
//import com.avolution.actor.core.ActorRef;
//import com.avolution.actor.core.ActorSystem;
//import com.avolution.actor.core.Props;
//import com.avolution.actor.router.RouterConfig;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 路由管理器
// */
//public class  RouterManager {
//    private final ActorSystem system;
//    private final Map<String, Router> routers;
//    private final AtomicInteger routerCounter;
//
//    public RouterManager(ActorSystem system) {
//        this.system = system;
//        this.routers = new ConcurrentHashMap<>();
//        this.routerCounter = new AtomicInteger(0);
//    }
//
//    /**
//     * 创建路由器
//     */
//    public Router createRouter(RouterConfig config, String name) {
//        String routerName = name != null ? name : generateRouterName();
//        Router router = new Router(system, config, routerName);
//        routers.put(routerName, router);
//        return router;
//    }
//
//    /**
//     * 获取路由器
//     */
//    public Optional<Router> getRouter(String name) {
//        return Optional.ofNullable(routers.get(name));
//    }
//
//    /**
//     * 移除路由器
//     */
//    public void removeRouter(String name) {
//        Optional.ofNullable(routers.remove(name))
//                .ifPresent(Router::shutdown);
//    }
//
//    /**
//     * 创建路由Actor
//     */
//    public ActorRef createRoutedActor(Props props, RouterConfig config, String name) {
//        Router router = createRouter(config, name);
//        return null;
////        return router.createRoutedActor(props);
//    }
//
//    /**
//     * 获取所有路由器
//     */
//    public Collection<Router> getAllRouters() {
//        return Collections.unmodifiableCollection(routers.values());
//    }
//
//    /**
//     * 关闭所有路由器
//     */
//    public void shutdown() {
//        routers.values().forEach(Router::shutdown);
//        routers.clear();
//    }
//
//    private String generateRouterName() {
//        return "router-" + routerCounter.incrementAndGet();
//    }
//
//    /**
//     * 路由器实现类
//     */
//    public static class Router {
//        private final ActorSystem system;
//        private final RouterConfig config;
//        private final String name;
//        private final List<ActorRef> routees;
//        private final Map<ActorRef, RouterMetrics> metrics;
//        private volatile boolean isShutdown;
//
//        Router(ActorSystem system, RouterConfig config, String name) {
//            this.system = system;
//            this.config = config;
//            this.name = name;
//            this.routees = new ArrayList<>();
//            this.metrics = new ConcurrentHashMap<>();
//            this.isShutdown = false;
//            initialize();
//        }
//
////        private void initialize() {
////            for (int i = 0; i < config.getPoolSize(); i++) {
////                addRoutee();
////            }
////        }
//
////        public ActorRef createRoutedActor(Props props) {
////            if (isShutdown) {
////                throw new IllegalStateException("Router is shutdown");
////            }
////            String path = "/router/" + name;
////            return system.actorOf(props.withRouter(this), path, null);
////        }
////
////        public void addRoutee() {
////            if (!isShutdown && routees.size() < config.getMaxPoolSize()) {
////                ActorRef routee = system.actorOf(
////                    Props.empty(),
////                    name + "-routee-" + routees.size(),
////                    null
////                );
////                routees.add(routee);
////                metrics.put(routee, new RouterMetrics());
////            }
////        }
////
////        public void removeRoutee(ActorRef routee) {
////            if (routees.remove(routee)) {
////                metrics.remove(routee);
////                system.stopActor(routee);
////            }
////        }
//
//        public ActorRef selectRoutee(Object message) {
//            return config.selectRoutee(routees, message);
//        }
//
//        public void updateMetrics(ActorRef routee, long processingTime) {
//            metrics.get(routee).update(processingTime);
//        }
//
//        public void shutdown() {
//            isShutdown = true;
//            routees.forEach(system::stopActor);
//            routees.clear();
//            metrics.clear();
//        }
//
//        // Getters
//        public String getName() { return name; }
//        public List<ActorRef> getRoutees() { return Collections.unmodifiableList(routees); }
//        public RouterConfig getConfig() { return config; }
//        public boolean isShutdown() { return isShutdown; }
//        public Map<ActorRef, RouterMetrics> getMetrics() { return Collections.unmodifiableMap(metrics); }
//    }
//
//    /**
//     * 路由度量收集器
//     */
//    public static class RouterMetrics {
//        private final AtomicInteger messageCount = new AtomicInteger(0);
//    }
//}