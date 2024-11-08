# Avolution

Avolution is a Java-based server framework designed to provide a robust and scalable solution for building high-performance applications. It leverages modern Java features and libraries to offer a flexible and efficient development experience.

## Installation

To install Avolution, follow these steps:

1. Clone the repository:
   ```sh
   git clone https://github.com/zerosoft/avolution.git
   ```

2. Navigate to the project directory:
   ```sh
   cd avolution
   ```

3. Build the project using Maven:
   ```sh
   mvn clean install
   ```

## Usage

To use Avolution in your project, follow these steps:

1. Add the following dependency to your `pom.xml` file:
   ```xml
   <dependency>
       <groupId>avolution</groupId>
       <artifactId>core</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. Create an instance of the `ActorSystem`:
   ```java
   ActorSystem system = ActorSystem.create("MySystem");
   ```

3. Define your actors by extending the `AbstractActor` class:
   ```java
   public class MyActor extends AbstractActor<String> {
       @Override
       public void onReceive(String message) {
           System.out.println("Received message: " + message);
       }
   }
   ```

4. Create and use actors in your application:
   ```java
   ActorRef<String> myActor = system.actorOf(Props.create(MyActor.class), "myActor");
   myActor.tell("Hello, Avolution!", ActorRef.noSender());
   ```

## Contributing

We welcome contributions to Avolution! If you would like to contribute, please follow these guidelines:

1. Fork the repository and create a new branch for your feature or bugfix.
2. Write tests for your changes and ensure all existing tests pass.
3. Submit a pull request with a clear description of your changes.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
