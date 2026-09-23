package {{package}}

import dev.ancaria.coderpack.api.SacredMod
import dev.ancaria.coderpack.api.event.Hero

/** {{description}} */
class {{class}} : SacredMod() {

    override fun onLoad() {
        // The hero was found: once per world load, and again when the player
        // switches character. Nothing before this can touch the player,
        // because before this there is no world.
        context.registry.eventRegistry.on(Hero::class.java) { hero ->
            context.log("Hero reached level ${hero.level}.")
        }
        context.log("Loaded.")
    }
}
