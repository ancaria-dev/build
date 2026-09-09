package dev.ancaria.coderpack.verify;

import java.util.List;
import java.util.Map;

/**
 * The descriptor: there is one, it says who the mod is, and its ranges cover a
 * loader this toolchain can vouch for.
 *
 * <p>Everything the loader does with a jar starts here. No descriptor and the
 * jar is skipped without a word about why. No {@code id} or {@code entrypoint}
 * and it is skipped as unparseable. An {@code api} range this contract is
 * outside of and it is refused by name. All three are silent at build time and
 * loud in a player log, which is the wrong way round.
 *
 * <p>{@code loader} is checked for syntax and no further. It names launcher
 * releases, and no toolchain can know which of those exist yet. A mod that
 * asks for a fix landing in the next one is writing something true about a
 * version this build has never seen.
 */
final class Declaration implements Check {

    @Override
    public void run(Mod mod, List<Finding> found) {
        if (!mod.described()) {
            found.add(Finding.error("declaration", "No " + Mod.DESCRIPTOR
                    + " inside the jar. The loader skips jars without it."));
            return;
        }
        Map<String, String> values = mod.values();

        String id = values.get("id");
        if (id == null || id.isBlank()) {
            found.add(Finding.error("declaration",
                    "No id. The loader can’t parse a descriptor without one."));
        } else if (!Ids.valid(id)) {
            found.add(Finding.warning("declaration", "id is “" + id
                    + "”. The launcher writes it into its enabled list, so use only"
                    + " lowercase letters, digits and hyphens."));
        }

        if (mod.entrypoint() == null) {
            found.add(Finding.error("declaration",
                    "No entrypoint. The loader has no class to start."));
        }

        String api = values.get("api");
        if (api == null || api.isBlank()) {
            found.add(Finding.error("declaration", "No “api” entry. The loader requires the"
                    + " API contract used to build the mod. Let the Gradle plugin write it."));
        } else {
            try {
                if (!Verifier.builds(api)) {
                    found.add(Finding.error("declaration", "api = “" + api + "”, but this"
                            + " toolchain builds API " + Verifier.API + ". Include that API"
                            + " in the range."));
                }
            } catch (IllegalArgumentException bad) {
                found.add(Finding.error("declaration", "API version range is invalid: "
                        + bad.getMessage() + ". A range looks like [1,2)."));
            }
        }

        String loader = values.get("loader");
        if (loader != null && !loader.isBlank()) {
            try {
                Ranges.range(loader);
            } catch (IllegalArgumentException bad) {
                found.add(Finding.error("declaration", "Loader version range is invalid: "
                        + bad.getMessage() + ". A range looks like [0.1.20,)."));
            }
        }

        String version = values.get("version");
        if (version == null || version.isBlank()) {
            found.add(Finding.warning("declaration",
                    "No version. The launcher will show this mod as version 0."));
        }

        // A conflict names another mod, and a name nothing can ever match is a
        // rule that silently does not apply. Not an error: the jar loads, and
        // the loader has no opinion on who it sits beside.
        for (String other : mod.conflicts()) {
            if (other.equals(id)) {
                found.add(Finding.warning("declaration",
                        "conflicts names this mod itself, which can’t happen."));
            } else if (!Ids.valid(other)) {
                found.add(Finding.warning("declaration", "conflicts names “" + other
                        + "”, which isn’t a valid mod id and matches nothing."));
            }
        }
    }
}
