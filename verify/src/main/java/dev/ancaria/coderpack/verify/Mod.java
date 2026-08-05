package dev.ancaria.coderpack.verify;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A mod jar, opened once and handed to every check.
 *
 * @param entries     every file in the jar, in the order the zip lists them
 * @param classes     by internal name, {@code demo/DemoMod}
 * @param declaration the descriptor as it was written, null when there is none
 * @param values      that descriptor parsed, empty when there is none
 */
record Mod(Path path, List<String> entries, Map<String, Klass> classes,
           String declaration, Map<String, String> values) {

    static final String DESCRIPTOR = "META-INF/declaration.toml";

    /** The API the loader hands the mod. A mod that packs its own is broken. */
    static final String API_PACKAGE = "dev/ancaria/coderpack/api/";

    boolean described() {
        return declaration != null;
    }

    /** The entrypoint as the descriptor spells it, or null when it says nothing. */
    String entrypoint() {
        String name = values.get("entrypoint");
        return name == null || name.isBlank() ? null : name;
    }

    /** Mods this one cannot sit beside, by id. Empty when it names none. */
    List<String> conflicts() {
        return Toml.list(values.get("conflicts"));
    }
}
