package dev.ancaria.coderpack.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a built mod jar and says what would go wrong when the loader picks it
 * up.
 *
 * <p>Two callers use it: the {@code verifySacredMod} task, which runs it on the
 * jar the Gradle plugin just packed, and {@code coderpack verify}, which runs it
 * on a path in a CI job with no Gradle anywhere. Both print the {@link Report}
 * the same way.
 *
 * <p>Nothing here loads a class. Every answer comes out of the bytecode, so a
 * static initialiser in a mod -- or in a jar that came from a stranger -- never
 * gets to run in the process doing the checking.
 */
public final class Verifier {

    /**
     * The API contract this toolchain builds against, and the only one the
     * loader beside it accepts.
     *
     * <p>The one copy in this repository. {@code Descriptor.API} in the Gradle
     * plugin reads it from here, so the number stamped into a mod and the number
     * this linter demands cannot drift apart -- a drift that would fail every
     * jar the plugin builds. Raising it is one edit here, one in
     * {@code Api.VERSION} in coderpack and one in {@code mods.API} in the
     * launcher.
     */
    public static final String API = "1";

    /**
     * The range {@code Descriptor} writes when a mod author says nothing: this
     * major and no other, which is what every mod meant back when the field was
     * a bare number.
     *
     * <p>Derived rather than typed, because a default that names a different
     * major than {@link #API} would stamp every jar this plugin builds with a
     * range its own linter then refuses.
     */
    public static final String API_RANGE = "[" + API + "," + (Integer.parseInt(API) + 1) + ")";

    /**
     * Whether a range in a descriptor covers the contract this toolchain builds.
     *
     * <p>Used twice: by the linter on a packed jar, and by the Gradle plugin on
     * what an author wrote, before it is written into a descriptor at all. A mod
     * author may narrow or widen the range they claim, but not to something this
     * toolchain cannot vouch for -- a build that compiles against API 1 and
     * declares API 2 is a claim nobody checked.
     *
     * @throws IllegalArgumentException when the text is not a range
     */
    public static boolean builds(String range) {
        return Ranges.range(range).has(Ranges.version(API));
    }

    private static final List<Check> CHECKS =
            List.of(new Declaration(), new Contents(), new Entrypoint(), new Listeners());

    private Verifier() {
    }

    public static Report verify(Path jar) {
        Mod mod;
        try {
            mod = Jar.read(jar);
        } catch (IOException notAJar) {
            return new Report(jar, List.of(Finding.error("jar",
                    "Can’t read this as a jar: " + notAJar.getMessage())));
        }
        List<Finding> found = new ArrayList<>();
        for (Check check : CHECKS) {
            check.run(mod, found);
        }
        return new Report(jar, List.copyOf(found));
    }

    /**
     * The descriptor inside a jar, parsed the way the loader parses it, or an
     * empty map when the jar carries none.
     *
     * <p>Here for the same reason {@link #API} is here. A registry index, the
     * launcher's list and this linter all describe the same jar, and a second
     * reader of the same file is a second answer to the same question. Only that
     * one entry is read: no class in the jar is opened, never mind loaded.
     */
    public static Map<String, String> describe(Path jar) throws IOException {
        String text = Jar.declaration(jar);
        return text == null ? Map.of() : Toml.parse(text);
    }

    /**
     * The same reader, on text that came from somewhere other than a jar.
     *
     * <p>What it understands is one line of {@code key = "value"} and nothing
     * else, which is the subset the loader and the launcher understand. Handing
     * it a file it was not meant for gets a map of whatever it recognised, never
     * an exception.
     */
    public static Map<String, String> parse(String text) {
        return Toml.parse(text);
    }

    /**
     * A descriptor value written as {@code ["a", "b"]}, split back into its
     * strings. {@code authors} and {@code conflicts} are the two that are.
     */
    public static List<String> list(String value) {
        return Toml.list(value);
    }
}
