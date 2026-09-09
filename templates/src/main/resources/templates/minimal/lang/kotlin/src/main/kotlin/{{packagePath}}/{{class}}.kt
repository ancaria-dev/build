package {{package}}

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.ktx.SacredMod

/** {{description}} */
class {{class}} : SacredMod() {

    /**
     * Runs once, with the loader's context as the receiver. The same object
     * stays available as `context` from anywhere else in the class.
     */
    override fun Context.load() {
        log("Loaded.")
    }
}
