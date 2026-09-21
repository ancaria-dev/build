package demo;

import dev.ancaria.coderpack.api.Priority;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Gold;
import dev.ancaria.coderpack.api.event.Hero;

/**
 * Listeners with one thing wrong each, and one that is right. They are package-private because
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

/** Correct: a decider returns its own event's mutation. */
final class Decider {

    @Subscribe
    public Gold.Mutation onGold(Gold gold) {
        return null;
    }
}

/** Another event's mutation, which the loader would fold into the wrong number. */
final class Crossed {

    @Subscribe
    public Gold.Mutation onHero(Hero hero) {
        return null;
    }
}

/** MONITOR watches, so its answer is thrown away. */
final class Watcher {

    @Subscribe(priority = Priority.MONITOR)
    public Gold.Mutation onGold(Gold gold) {
        return null;
    }
}
