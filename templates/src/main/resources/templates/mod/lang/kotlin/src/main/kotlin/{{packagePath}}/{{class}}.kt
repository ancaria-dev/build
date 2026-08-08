package {{package}}

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.api.SacredMod
import dev.ancaria.coderpack.api.Subscribe
import dev.ancaria.coderpack.api.event.Hero

/** {{description}} */
class {{class}} : SacredMod {

    private lateinit var context: Context

    override fun onLoad(context: Context) {
        this.context = context
        // Every public @Subscribe method on this object becomes a listener.
        context.events().register(this)
        context.log("Loaded.")
    }

    /**
     * The hero was found: once per world load, and again when the player
     * switches character. Nothing before this can touch the player, because
     * before this there is no world.
     */
    @Subscribe
    fun onHero(hero: Hero) {
        context.log("Hero reached level " + hero.level() + ".")
    }
}
