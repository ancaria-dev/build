package dev.ancaria.coderpack.api;

/**
 * Stub. What a mod reaches through {@code getContext()}.
 *
 * <p>Wider than the linter's own fixtures need, because the scaffolder compiles
 * a generated mod against these files too and a generated entrypoint logs and
 * registers its listeners through here.
 */
public interface Context {

    void log(String message);

    Registry getRegistry();
}
