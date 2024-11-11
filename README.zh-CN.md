# Avolution

Avolution 是一个基于 Java 21 开发的高性能 Actor 模型框架,专注于构建可扩展的分布式应用系统。它提供了完整的 Actor 编程模型实现,并集成了网络通信、监控指标等企业级特性。

## 核心特性

1. Actor 系统
 ```
 完整的 Actor 生命周期管理
 灵活的消息传递机制
 支持 Ask 模式的同步调用
 内置死信处理机制
 ```
2. 监督策略
 ```
One-for-One 策略
All-for-One 策略
可自定义错误处理指令
 ```
3. 消息邮箱
 ```
高性能消息队列
系统消息优先级处理
可配置的消息吞吐量
 ```
4. 性能监控
 ```
详细的度量指标收集
实时性能统计
可视化监控支持
 ```
## 快速开始

1. 添加依赖:`pom.xml` :
   ```xml
   <dependency>
       <groupId>avolution</groupId>
       <artifactId>core</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. 启动系统 `ActorSystem`:
   ```java
     ActorSystem system = ActorSystem.create("MySystem");
     ActorRef<HelloActorMessage> actor = system.actorOf(Props.create(HelloActor.class), "hello");
     actor.tell(new HelloActorMessage.Hello(), ActorRef.noSender());
   ```

3. 创建 Actor: `AbstractActor` class:
   ```java
      public class HelloActor extends AbstractActor<HelloActorMessage> {
   
       @OnReceive(HelloActorMessage.Hello.class)
       public void handleHelloMessage(HelloActorMessage.Hello message) {
           System.out.println("Hello, Actor! " + message);
       }
   
       @OnReceive(HelloActorMessage.World.class) 
       public void handleWorldMessage(HelloActorMessage.World message) {
           System.out.println("World, Actor! " + message);
           getSender().tell("OK", getSelf());
       }
   }
   ```

 ## 应用场景:
  ```
  分布式系统开发
  高并发服务器
  实时通信应用
  游戏服务器开发
 ```
## 示例代码

```java
    @Test
    void testActorTimer() throws Exception {
        ActorSystem system = ActorSystem.create("TimerTestSystem");

        try {
            // 创建带定时器的Actor
            ActorRef<TimerHelloActor.Message> timerActor =
                    system.actorOf(Props.create(TimerHelloActor.class), "timer-hello");

            // 启动定时器，每500毫秒执行一次
            timerActor.tell(
                    new TimerHelloActor.StartTimer(Duration.ofMillis(500)),
                    ActorRef.noSender()
            );

            // 等待几个定时器周期
            Thread.sleep(2000);

            // 检查定时器执行次数
            CompletableFuture<Object> future = timerActor.ask(
                    new TimerHelloActor.GetTimerCount(),
                    Duration.ofSeconds(1)
            );

            Integer count = (Integer) future.get();
            System.out.println(count);
            assertTrue(count >= 3, "Timer should have ticked at least 3 times");

        } finally {
            // 关闭Actor系统
            system.terminate();
        }
    }

   ```
