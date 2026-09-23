package demo;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;

/** A mod built for API 2, when SacredMod was an interface. */
public final class LegacyMod implements SacredMod {

    @Override
    public void onLoad(Context context) {
    }
}
