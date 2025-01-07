package com.avolution.actor.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseActorTest {
    protected ActorSystem system;

    @BeforeEach
    void setUp() {
        system = ActorSystem.create("test-system");
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.terminate();
        }
    }
} 