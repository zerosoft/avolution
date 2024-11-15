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

## Project Directory Structure

The project directory structure is organized as follows:

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

- `core/src/main/java/com/avolution/actor`: Contains the core actor framework classes.
- `core/src/main/java/com/avolution/net`: Contains network-related classes for TCP and UDP communication.
- `core/src/main/java/com/avolution/service`: Contains service management classes.
- `core/src/test/java/com/avolution`: Contains test classes for the actor framework and network communication.
- `core/src/main/resources`: Contains resource files for the project.
- `core/src/test/resources`: Contains test resource files.

## Project Architecture

The project's architecture is designed to provide a high-performance, scalable actor model framework. The key components and their roles are as follows:

- `AbstractActor`: The base class for all actors, providing core actor functionality.
- `ActorSystem`: The main class for creating and managing actors and their lifecycle.
- `ActorContext`: Provides context for actors, including their state and message handling.
- `Props`: A configuration class for creating actors with specific settings.
- `Mailbox`: A high-performance message queue for actors.
- `Dispatcher`: Responsible for dispatching messages to actors.
- `SupervisorStrategy`: Defines strategies for handling actor failures.
- `DeadLetterActor`: Handles messages that cannot be delivered to their intended recipients.
- `VirtualThreadScheduler`: A scheduler for managing actor execution using virtual threads.

## Contributing

We welcome contributions to the Avolution project! To contribute, please follow these guidelines:

1. Fork the repository and create a new branch for your feature or bugfix.
2. Write tests for your changes and ensure all existing tests pass.
3. Submit a pull request with a clear description of your changes.

### Reporting Issues

If you encounter any issues or have questions, please open an issue on the GitHub repository. Provide as much detail as possible to help us understand and resolve the issue.

### Submitting Pull Requests

When submitting a pull request, please ensure the following:

- Your code follows the project's coding standards and conventions.
- You have written tests for your changes.
- You have updated any relevant documentation.

Thank you for contributing to Avolution!
