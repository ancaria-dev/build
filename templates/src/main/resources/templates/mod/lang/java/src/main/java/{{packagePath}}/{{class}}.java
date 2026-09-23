package {{package}};

import dev.ancaria.coderpack.api.SacredMod;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Hero;

/** {{description}} */
public final class {{class}} extends SacredMod {

    @Override
    public void onLoad() {
        // Every public @Subscribe method on this object becomes a listener.
        getContext().getRegistry().getEventRegistry().register(this);
        getContext().log("Loaded.");
    }

    /**
     * The hero was found: once per world load, and again when the player
     * switches character. Nothing before this can touch the player, because
     * before this there is no world.
     */
    @Subscribe
    public void onHero(Hero hero) {
        getContext().log("Hero reached level " + hero.getLevel() + ".");
    }
}
