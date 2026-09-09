package dev.ancaria.coderpack.verify;

import java.util.List;
import java.util.Locale;

/** What the jar carries that it should not, and what it was compiled for. */
final class Contents implements Check {

    /** Everything the loader itself provides, next to the API. */
    private static final String ZYGOTE = "dev/ancaria/coderpack/zygote/";

    /** Class file 65 is Java 21, which is what the loader and the plugin target. */
    private static final int TARGET = 65;

    @Override
    public void run(Mod mod, List<Finding> found) {
        api(mod, found);
        signatures(mod, found);
        bytecode(mod, found);
    }

    /**
     * The API must come from the loader and only from the loader. A mod is
     * loaded by its own class loader. A copy of the API inside the jar is a
     * second class with the same name, and the first symptom is a
     * ClassCastException between two types that are obviously identical.
     */
    private static void api(Mod mod, List<Finding> found) {
        List<String> packed = classesUnder(mod, Mod.API_PACKAGE);
        if (!packed.isEmpty()) {
            found.add(Finding.error("api", packed.size() + " API "
                    + (packed.size() == 1 ? "class is" : "classes are") + " inside the"
                    + " jar, starting with " + packed.getFirst() + "; the loader provides"
                    + " them and two copies of one type do not compare equal. Depend on"
                    + " the API with compileOnly"));
        }
        List<String> loader = classesUnder(mod, ZYGOTE);
        if (!loader.isEmpty()) {
            found.add(Finding.warning("api", loader.size() + " loader "
                    + (loader.size() == 1 ? "class is" : "classes are") + " inside"
                    + " the jar, starting with " + loader.getFirst() + "; nothing loads"
                    + " them and they belong to the zygote"));
        }
    }

    /**
     * A jar signed somewhere else, unpacked into this one, keeps its signature
     * files while its digests no longer describe anything. The class loader
     * rejects the lot with a SecurityException.
     */
    private static void signatures(Mod mod, List<Finding> found) {
        for (String entry : mod.entries()) {
            String upper = entry.toUpperCase(Locale.ROOT);
            if (upper.startsWith("META-INF/") && upper.lastIndexOf('/') == "META-INF".length()
                    && (upper.endsWith(".SF") || upper.endsWith(".DSA")
                        || upper.endsWith(".RSA"))) {
                found.add(Finding.error("signature", entry + " is a signature file from"
                        + " another jar; the class loader refuses every class next to one"
                        + " whose digest no longer matches"));
                return;
            }
        }
    }

    /**
     * The loader targets Java 21 and starts on whatever JVM the player has. A
     * class file above that runs on the machine that built it and throws
     * UnsupportedClassVersionError on somebody else.
     */
    private static void bytecode(Mod mod, List<Finding> found) {
        Klass newest = null;
        int above = 0;
        for (Klass klass : mod.classes().values()) {
            if (klass.version() > TARGET) {
                above++;
                // By name where the version ties, so two runs over one jar say
                // the same thing. The classes arrive in hash order.
                if (newest == null || klass.version() > newest.version()
                        || (klass.version() == newest.version()
                            && klass.name().compareTo(newest.name()) < 0)) {
                    newest = klass;
                }
            }
        }
        if (newest != null) {
            found.add(Finding.warning("bytecode", above + (above == 1 ? " class is" : " classes are")
                    + " class file"
                    + " version " + newest.version() + " (Java " + (newest.version() - 44)
                    + "), starting with " + newest.dotted() + "; the loader targets Java "
                    + (TARGET - 44) + " and a player running exactly that cannot load them"));
        }
    }

    private static List<String> classesUnder(Mod mod, String prefix) {
        return mod.entries().stream()
                  .filter(entry -> entry.startsWith(prefix) && entry.endsWith(".class"))
                  .sorted()
                  .toList();
    }
}
