package demo;

import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Hero;

/**
 * Four listeners, each with one thing wrong. They are package-private because
 * the bus reads the public methods of whatever object a mod registers, and the
 * class holding them does not have to be public for that.
 */
final class Listen {
}

/** Two parameters where the bus takes exactly one. */
final class Arity {

    @Subscribe
    public void onHero(Hero first, Hero second) {
    }
}

/** A parameter that is not an event, so the bus never dispatches to it. */
final class NotEvent {

    @Subscribe
    public void onHero(String text) {
    }
}

/** A return value that goes nowhere. */
final class Returns {

    @Subscribe
    public String onHero(Hero hero) {
        return "";
    }
}

/** Not public, so the bus never sees it at all. */
final class Quiet {

    @Subscribe
    void onHero(Hero hero) {
    }
}
