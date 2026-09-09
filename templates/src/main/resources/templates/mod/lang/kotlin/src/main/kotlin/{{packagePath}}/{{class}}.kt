package {{package}}

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.api.event.Hero
import dev.ancaria.coderpack.ktx.SacredMod
import dev.ancaria.coderpack.ktx.events
import dev.ancaria.coderpack.ktx.level
import dev.ancaria.coderpack.ktx.on

/** {{description}} */
class {{class}} : SacredMod() {

    /**
     * Runs once, with the loader's context as the receiver, which is why `log`
     * and `events` read as bare calls. The same object stays available as
     * `context` from anywhere else in the class.
     */
    override fun Context.load() {
        events {
            // The hero was found: once per world load, and again when the
            // player switches character. Nothing before this can touch the
            // player, because before this there is no world.
            on<Hero> { log("Hero reached level ${it.level}.") }
        }
        log("Loaded.")
    }
}
