package demo;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;

/**
 * Package-private, with a public constructor so that visibility is the only
 * thing wrong with it.
 */
class HiddenMod implements SacredMod {

    public HiddenMod() {
    }

    @Override
    public void onLoad(Context context) {
    }
}
