package com.avolution.actor;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Supervisor {
    private static final Logger logger = Logger.getLogger(Supervisor.class.getName());
    private final List<AbstractActor> actors;
    private final SupervisionStrategy strategy;

    public Supervisor(SupervisionStrategy strategy) {
        this.actors = new ArrayList<>();
        this.strategy = strategy;
    }

    public void addActor(AbstractActor actor) {
        actors.add(actor);
    }

    public void handleFailure(AbstractActor actor, Throwable cause) {
        logger.severe("Actor failed: " + actor.getId() + ", cause: " + cause.getMessage());
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

    private void restartActor(AbstractActor actor) {
        logger.info("Restarting actor: " + actor.getId());
        actor.restart();
    }

    private void stopActor(AbstractActor actor) {
        logger.info("Stopping actor: " + actor.getId());
        actor.stop();
    }

    private void resumeActor(AbstractActor actor) {
        logger.info("Resuming actor: " + actor.getId());
        actor.resume();
    }

    public enum SupervisionStrategy {
        RESTART,
        STOP,
        RESUME
    }
}
