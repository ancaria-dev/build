package dev.ancaria.coderpack.api;

/** Stub. What registering a listener hands back, so it can be taken off again. */
@FunctionalInterface
public interface Handle {

    void unregister();
}
