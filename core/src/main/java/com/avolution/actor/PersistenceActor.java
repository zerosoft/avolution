package com.avolution.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import java.io.*;

public class PersistenceActor extends AbstractActor {

    private final String persistenceFilePath;

    public PersistenceActor(String persistenceFilePath) {
        this.persistenceFilePath = persistenceFilePath;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SaveState.class, this::saveState)
                .match(RestoreState.class, this::restoreState)
                .build();
    }

    private void saveState(SaveState saveState) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(persistenceFilePath))) {
            oos.writeObject(saveState.state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreState(RestoreState restoreState) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(persistenceFilePath))) {
            Object state = ois.readObject();
            getSender().tell(new StateRestored(state), getSelf());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Props props(String persistenceFilePath) {
        return Props.create(PersistenceActor.class, () -> new PersistenceActor(persistenceFilePath));
    }

    public static class SaveState {
        public final Object state;

        public SaveState(Object state) {
            this.state = state;
        }
    }

    public static class RestoreState {
    }

    public static class StateRestored {
        public final Object state;

        public StateRestored(Object state) {
            this.state = state;
        }
    }
}
