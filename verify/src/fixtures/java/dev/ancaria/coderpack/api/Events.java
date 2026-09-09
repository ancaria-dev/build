package dev.ancaria.coderpack.api;

import dev.ancaria.coderpack.api.event.Event;

import java.util.function.Consumer;

/**
 * Stub. Enough of the bus for a mod to hand it a listener, either way in.
 *
 * <p>Both ways, because a generated mod uses both: a Java entrypoint registers
 * an annotated object, and a Kotlin one calls the lambda path through the
 * extensions in {@code dev.ancaria.coderpack.ktx}.
 */
public interface Events {

    void register(Object listener);

    <E extends Event> Handle on(Class<E> type, Priority priority,
                                boolean ignoreCancelled, Consumer<E> listener);
}
