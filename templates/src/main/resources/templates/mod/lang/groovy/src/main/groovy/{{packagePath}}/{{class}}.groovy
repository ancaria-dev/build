package {{package}}

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.api.SacredMod
import dev.ancaria.coderpack.api.Subscribe
import dev.ancaria.coderpack.api.event.Hero
import groovy.transform.CompileStatic

/** {{description}} */
// Static compilation: the same dispatch Java gets, and a typo in an event name
// is a compile error rather than a MissingMethodException mid-game. Drop the
// annotation on a class that wants Groovy's dynamic half.
@CompileStatic
class {{class}} implements SacredMod {

    private Context context

    @Override
    void onLoad(Context context) {
        this.context = context
        // Every public @Subscribe method on this object becomes a listener.
        context.events().register(this)
        context.log('Loaded.')
    }

    /**
     * The hero was found: once per world load, and again when the player
     * switches character. Nothing before this can touch the player, because
     * before this there is no world.
     */
    @Subscribe
    void onHero(Hero hero) {
        context.log('Hero reached level ' + hero.level() + '.')
    }
}
