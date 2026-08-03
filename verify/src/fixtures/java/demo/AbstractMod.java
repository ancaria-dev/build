package demo;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;

/** A mod nobody can instantiate. */
public abstract class AbstractMod implements SacredMod {

    @Override
    public void onLoad(Context context) {
    }
}
