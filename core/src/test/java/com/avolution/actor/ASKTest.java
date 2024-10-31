package com.avolution.actor;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class ASKTest {

    @Test
    public void testAskMethod() throws Exception {
        String name="214";
        String path =STR."root\{File.separator}\{name}";
        System.out.println(path);

        IActorSystem system = ActorSystem.create("testSystem");
        ActorRef actorRef = system.actorOf(Props.create(TestActor.class), "testActor");

//        CompletableFuture<String> future = actorRef.ask("Hello", 1, TimeUnit.SECONDS);

//        assertEquals("Hello Response", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testAskMethodTimeout() {
        IActorSystem system = ActorSystem.create("testSystem");
        ActorRef actorRef = system.actorOf(Props.create(TestActor.class), "testActor");

//        CompletableFuture<String> future = actorRef.ask("Hello", 500, TimeUnit.MILLISECONDS);

//        assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.SECONDS));
    }

    public static class TestActor extends Actor {
        @Override
        protected void receive(Object message) {
            if (message instanceof String) {
                getSender().tellMessage(message + " Response", self());
            }
        }
    }
}
