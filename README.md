# Avolution

[中文版本](README.zh-CN.md)

Avolution is a high-performance Actor model framework developed based on Java 21, focusing on building scalable distributed application systems. It provides a complete Actor programming model implementation and integrates enterprise-level features such as network communication and monitoring metrics.

## Core Features

1. Actor System
 ```
 Complete Actor lifecycle management
 Flexible message passing mechanism
 Support for synchronous calls in Ask mode
 Built-in dead letter handling mechanism
 ```
2. Supervision Strategy
 ```
One-for-One strategy
All-for-One strategy
Customizable error handling directives
 ```
3. Message Mailbox
 ```
High-performance message queue
System message priority handling
Configurable message throughput
 ```
4. Performance Monitoring
 ```
Detailed metric collection
Real-time performance statistics
Visualization monitoring support
 ```
## Quick Start

1. Add dependency in `pom.xml`:
   ```xml
   <dependency>
       <groupId>avolution</groupId>
       <artifactId>core</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. Start the system `ActorSystem`:
   ```java
     ActorSystem system = ActorSystem.create("MySystem");
     ActorRef<HelloActorMessage> actor = system.actorOf(Props.create(HelloActor.class), "hello");
     actor.tell(new HelloActorMessage.Hello(), ActorRef.noSender());
   ```

3. Create Actor: `AbstractActor` class:
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

 ## Application Scenarios:
  ```
  Distributed system development
  High-concurrency servers
  Real-time communication applications
  Game server development
 ```
## Example Code

```java
    @Test
    void testActorTimer() throws Exception {
        ActorSystem system = ActorSystem.create("TimerTestSystem");

        try {
            // Create an Actor with a timer
            ActorRef<TimerHelloActor.Message> timerActor =
                    system.actorOf(Props.create(TimerHelloActor.class), "timer-hello");

            // Start the timer, execute every 500 milliseconds
            timerActor.tell(
                    new TimerHelloActor.StartTimer(Duration.ofMillis(500)),
                    ActorRef.noSender()
            );

            // Wait for a few timer cycles
            Thread.sleep(2000);

            // Check the timer execution count
            CompletableFuture<Object> future = timerActor.ask(
                    new TimerHelloActor.GetTimerCount(),
                    Duration.ofSeconds(1)
            );

            Integer count = (Integer) future.get();
            System.out.println(count);
            assertTrue(count >= 3, "Timer should have ticked at least 3 times");

        } finally {
            // Terminate the Actor system
            system.terminate();
        }
    }

   ```
