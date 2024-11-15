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

## 项目目录结构

项目目录结构如下：

```
/core
  /src
    /main
      /java
        /com
          /avolution
            /actor
              /concurrent
              /config
              /core
              /dispatch
              /dispatchers
              /exception
              /lifecycle
              /mailbox
              /message
              /metrics
              /pattern
              /router
              /routing
              /strategy
              /supervision
              /system
              /util
            /net
              /tcp
              /udp
            /service
    /test
      /java
        /com
          /avolution
            /actor
            /net
              /tcp
              /udp
            /service
  /resources
  /test
    /resources
```

- `core/src/main/java/com/avolution/actor`: 包含核心 actor 框架类。
- `core/src/main/java/com/avolution/net`: 包含用于 TCP 和 UDP 通信的网络相关类。
- `core/src/main/java/com/avolution/service`: 包含服务管理类。
- `core/src/test/java/com/avolution`: 包含 actor 框架和网络通信的测试类。
- `core/src/main/resources`: 包含项目的资源文件。
- `core/src/test/resources`: 包含测试资源文件。

## 项目架构

项目的架构旨在提供一个高性能、可扩展的 actor 模型框架。关键组件及其角色如下：

- `AbstractActor`: 所有 actor 的基类，提供核心 actor 功能。
- `ActorSystem`: 创建和管理 actor 及其生命周期的主要类。
- `ActorContext`: 提供 actor 的上下文，包括其状态和消息处理。
- `Props`: 用于创建具有特定设置的 actor 的配置类。
- `Mailbox`: actor 的高性能消息队列。
- `Dispatcher`: 负责将消息分发给 actor。
- `SupervisorStrategy`: 定义处理 actor 故障的策略。
- `DeadLetterActor`: 处理无法传递给预期接收者的消息。
- `VirtualThreadScheduler`: 使用虚拟线程管理 actor 执行的调度器。

## 贡献

我们欢迎对 Avolution 项目的贡献！要贡献，请遵循以下指南：

1. Fork 仓库并为您的功能或 bug 修复创建一个新分支。
2. 为您的更改编写测试并确保所有现有测试通过。
3. 提交一个包含更改清晰描述的 pull request。

### 报告问题

如果您遇到任何问题或有疑问，请在 GitHub 仓库上打开一个 issue。请尽可能提供详细信息，以帮助我们理解和解决问题。

### 提交 Pull Request

提交 pull request 时，请确保以下内容：

- 您的代码遵循项目的编码标准和约定。
- 您已为更改编写了测试。
- 您已更新任何相关文档。

感谢您为 Avolution 做出的贡献！
