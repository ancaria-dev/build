package dev.ancaria.coderpack.api;

/**
 * Enough of the real interface for a fixture to implement it.
 *
 * <p>The API is a stub here rather than the published artifact so the tests
 * need no network and no credentials for a package registry. Only the name
 * matters: the linter reads the name out of the bytecode and never loads it.
 */
public interface SacredMod {

    void onLoad(Context context);
}
