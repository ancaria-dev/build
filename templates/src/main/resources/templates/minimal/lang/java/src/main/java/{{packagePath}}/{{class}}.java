package {{package}};

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;

/** {{description}} */
public final class {{class}} implements SacredMod {

    @Override
    public void onLoad(Context context) {
        context.log("Loaded.");
    }
}
