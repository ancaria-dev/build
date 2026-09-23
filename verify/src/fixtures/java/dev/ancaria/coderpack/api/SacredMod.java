package dev.ancaria.coderpack.api;

/**
 * Enough of the real class for a fixture to extend it.
 *
 * <p>The API is a stub here rather than the published artifact so the tests
 * need no network and no credentials for a package registry. Only the name and
 * the shape matter: the linter reads the name out of the bytecode and never
 * loads it, and a generated mod only has to compile against it.
 */
public abstract class SacredMod {

    protected SacredMod() {
    }

    public final Context getContext() {
        throw new IllegalStateException("A stub has no context.");
    }

    public void onLoad() {
    }

    public void onUnload() {
    }
}
