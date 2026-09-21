package dev.ancaria.coderpack.api.event;

/** Stub of one decidable event, which is all a fixture listener needs. */
public final class Gold extends Event implements Decides<Gold.Mutation> {

    public static final class Mutation extends EventMutation {
    }
}
