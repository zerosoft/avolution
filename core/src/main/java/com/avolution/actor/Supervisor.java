package com.avolution.actor;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Supervisor {
    private static final Logger logger = Logger.getLogger(Supervisor.class.getName());
    private final List<Actor> actors;
    private final SupervisionStrategy strategy;

    public Supervisor(SupervisionStrategy strategy) {
        this.actors = new ArrayList<>();
        this.strategy = strategy;
    }

    public void addActor(Actor actor) {
        actors.add(actor);
    }

    public void handleFailure(Actor actor, Throwable cause) {
        logger.severe("Actor failed: " + actor + ", cause: " + cause.getMessage());
        switch (strategy) {
            case RESTART:
                restartActor(actor);
                break;
            case STOP:
                stopActor(actor);
                break;
            case RESUME:
                resumeActor(actor);
                break;
        }
    }

    private void restartActor(Actor actor) {
        logger.info("Restarting actor: " + actor);
        // Implement restart logic
    }

    private void stopActor(Actor actor) {
        logger.info("Stopping actor: " + actor);
        // Implement stop logic
    }

    private void resumeActor(Actor actor) {
        logger.info("Resuming actor: " + actor);
        // Implement resume logic
    }

    public enum SupervisionStrategy {
        RESTART,
        STOP,
        RESUME
    }
}
