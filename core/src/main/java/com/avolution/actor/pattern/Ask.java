//package com.avolution.actor.pattern;
//
//import com.avolution.actor.core.AbstractActor;
//import com.avolution.actor.core.ActorRef;
//import com.avolution.actor.core.ActorSystem;
//import com.avolution.actor.core.Props;
//import com.avolution.actor.message.Envelope;
//
//import java.time.Duration;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionStage;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
///**
// * Ask模式的实现类，用于请求-响应模式的通信
// */
//public class Ask {
//    private final ActorSystem system;
//
//    public Ask(ActorSystem system) {
//        this.system = system;
//    }
//
//    /**
//     * 发送请求并等待响应
//     *
//     * @param target 目标Actor
//     * @param message 消息
//     * @param timeout 超时时间
//     * @param <T> 响应类型
//     * @return 包含响应的CompletionStage
//     */
//    public <T> CompletionStage<T> ask(ActorRef target, Object message, Duration timeout) {
//        CompletableFuture<T> future = new CompletableFuture<>();
//        String correlationId = UUID.randomUUID().toString();
//
//        // 创建临时Actor处理响应
//        Props tempActorProps = Props.create(() -> new TemporaryActor<>(future, correlationId));
//        ActorRef tempActor = system.createTempActor(tempActorProps);
//
//        // 发送消息
//        target.tell(new AskMessage<>(message, tempActor, correlationId), tempActor);
//
//        // 设置超时
//        system.scheduler().schedule(() -> {
//            if (!future.isDone()) {
//                future.completeExceptionally(
//                    new AskTimeoutException("Ask timeout after " + timeout.toMillis() + "ms")
//                );
//                tempActor.tell(PoisonPill.INSTANCE, ActorRef.noSender());
//            }
//        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
//
//        return future;
//    }
//
//    /**
//     * 临时Actor，用于处理响应
//     */
//    private static class TemporaryActor<T> extends AbstractActor {
//        private final CompletableFuture<T> future;
//        private final String correlationId;
//
//        public TemporaryActor(CompletableFuture<T> future, String correlationId) {
//            this.future = future;
//            this.correlationId = correlationId;
//        }
//
//        @Override
//        public void onReceive(Envelope envelope) {
//            Object message = envelope.getMessage();
//
//            if (message instanceof Response) {
//                Response<?> response = (Response<?>) message;
//                if (correlationId.equals(response.correlationId)) {
//                    //noinspection unchecked
//                    future.complete((T) response.result);
//                    context().stop(self());
//                }
//            } else if (message instanceof PoisonPill) {
//                context().stop(self());
//            }
//        }
//
//        @Override
//        public void postStop() {
//            if (!future.isDone()) {
//                future.completeExceptionally(
//                    new ActorTerminatedException("Actor terminated before receiving response")
//                );
//            }
//        }
//    }
//
//    /**
//     * Ask消息包装类
//     */
//    public static class AskMessage<T> {
//        private final T message;
//        private final ActorRef replyTo;
//        private final String correlationId;
//
//        public AskMessage(T message, ActorRef replyTo, String correlationId) {
//            this.message = message;
//            this.replyTo = replyTo;
//            this.correlationId = correlationId;
//        }
//
//        public T getMessage() {
//            return message;
//        }
//
//        public ActorRef getReplyTo() {
//            return replyTo;
//        }
//
//        public String getCorrelationId() {
//            return correlationId;
//        }
//    }
//
//    /**
//     * 响应消息包装类
//     */
//    public static class Response<T> {
//        private final T result;
//        private final String correlationId;
//
//        public Response(T result, String correlationId) {
//            this.result = result;
//            this.correlationId = correlationId;
//        }
//
//        public T getResult() {
//            return result;
//        }
//
//        public String getCorrelationId() {
//            return correlationId;
//        }
//    }
//
//    /**
//     * Ask超时异常
//     */
//    public static class AskTimeoutException extends RuntimeException {
//        public AskTimeoutException(String message) {
//            super(message);
//        }
//    }
//
//    /**
//     * Actor终止异常
//     */
//    public static class ActorTerminatedException extends RuntimeException {
//        public ActorTerminatedException(String message) {
//            super(message);
//        }
//    }
//
//    /**
//     * 终止消息
//     */
//    private static class PoisonPill {
//        static final PoisonPill INSTANCE = new PoisonPill();
//        private PoisonPill() {}
//    }
//}