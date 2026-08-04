package demo;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;

/** Wants an argument the loader has no way to pass. */
public final class NoCtor implements SacredMod {

    public NoCtor(String name) {
    }

    @Override
    public void onLoad(Context context) {
    }
}
