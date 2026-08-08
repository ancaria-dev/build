package {{package}};

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Hero;

/** {{description}} */
public final class {{class}} implements SacredMod {

    private Context context;

    @Override
    public void onLoad(Context context) {
        this.context = context;
        // Every public @Subscribe method on this object becomes a listener.
        context.events().register(this);
        context.log("Loaded.");
    }

    /**
     * The hero was found: once per world load, and again when the player
     * switches character. Nothing before this can touch the player, because
     * before this there is no world.
     */
    @Subscribe
    public void onHero(Hero hero) {
        context.log("Hero reached level " + hero.level() + ".");
    }
}
