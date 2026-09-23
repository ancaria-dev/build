package dev.ancaria.coderpack.api;

import dev.ancaria.coderpack.api.event.Event;

import java.util.List;
import java.util.function.Consumer;

/**
 * Stub. Enough of the bus for a mod to hand it a listener, either way in.
 *
 * <p>Both ways, because a generated mod uses both: a Java or Groovy entrypoint
 * registers an annotated object, and a Kotlin one registers a lambda.
 */
public interface EventRegistry {

    List<Handle> register(Object listener);

    <E extends Event> Handle on(Class<E> type, Consumer<E> listener);
}
