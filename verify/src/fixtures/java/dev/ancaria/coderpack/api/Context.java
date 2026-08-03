package dev.ancaria.coderpack.api;

/**
 * Stub. What a mod is handed on load.
 *
 * <p>Wider than the linter's own fixtures need, because the scaffolder compiles
 * a generated mod against these files too and a generated entrypoint registers
 * its listeners here.
 */
public interface Context {

    String id();

    Events events();

    void log(String message);
}
