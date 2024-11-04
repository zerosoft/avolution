package com.avolution.actor.dispatchers;

public interface Dispatcher {

    void dispatch(Runnable message);

    String name();
}
