package {{package}}

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.api.SacredMod

/** {{description}} */
class {{class}} : SacredMod {

    override fun onLoad(context: Context) {
        context.log("Loaded.")
    }
}
