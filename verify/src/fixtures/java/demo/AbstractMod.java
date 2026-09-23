package demo;

import dev.ancaria.coderpack.api.SacredMod;

/** A mod nobody can instantiate, and a base a concrete one can extend. */
public abstract class AbstractMod extends SacredMod {

    @Override
    public void onLoad() {
    }
}
