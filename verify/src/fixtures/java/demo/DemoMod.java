package demo;

import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Hero;

/** A mod that breaks no rule. Every other fixture is this one with one thing wrong. */
public final class DemoMod extends SacredMod {

    @Override
    public void onLoad() {
    }

    @Subscribe
    public void onHero(Hero hero) {
    }
}
