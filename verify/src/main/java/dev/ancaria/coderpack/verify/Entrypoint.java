package dev.ancaria.coderpack.verify;

import java.util.List;

/**
 * The class the descriptor names: it is in the jar, the loader can build one,
 * and what it builds is a mod.
 *
 * <p>The loader does {@code loadClass(entrypoint).getDeclaredConstructor()
 * .newInstance()} and then asks whether the result is a {@code SacredMod}. Each
 * step here is one of the ways that line ends in a stack trace in a player log
 * instead of a loaded mod.
 */
final class Entrypoint implements Check {

    private static final String SACRED_MOD = "dev/ancaria/coderpack/api/SacredMod";

    @Override
    public void run(Mod mod, List<Finding> found) {
        String named = mod.entrypoint();
        if (named == null) {
            // Declaration has already said the descriptor names nothing.
            return;
        }
        Klass klass = mod.classes().get(named.replace('.', '/'));
        if (klass == null) {
            found.add(Finding.error("entrypoint", named + " isn’t in the jar. The loader"
                    + " looks it up by this exact name."));
            return;
        }
        if (!klass.parsed()) {
            found.add(Finding.warning("entrypoint", named + " uses class file version "
                    + klass.version() + ", which this linter can’t read. No other checks"
                    + " were run on it."));
            return;
        }
        if (!klass.isPublic()) {
            found.add(Finding.error("entrypoint", named + " isn’t public. The loader is"
                    + " in another package and can’t access it."));
        }
        if (klass.isAbstract()) {
            found.add(Finding.error("entrypoint", named + " is abstract and can’t be instantiated."));
        }
        constructor(named, klass, found);
        implementation(mod, named, klass, found);
    }

    private static void constructor(String named, Klass klass, List<Finding> found) {
        Klass.Meth empty = klass.methods().stream()
                                .filter(method -> "<init>".equals(method.name()))
                                .filter(method -> "()V".equals(method.descriptor()))
                                .findFirst().orElse(null);
        if (empty == null) {
            found.add(Finding.error("entrypoint", named + " has no no-argument"
                    + " constructor. The loader calls getDeclaredConstructor() without arguments."));
        } else if (!empty.isPublic()) {
            found.add(Finding.error("entrypoint", named + " has a non-public no-argument"
                    + " constructor that the loader can’t call."));
        }
    }

    private static void implementation(Mod mod, String named, Klass klass,
                                       List<Finding> found) {
        Boolean sacred = sacred(mod, klass);
        if (sacred == null) {
            found.add(Finding.warning("entrypoint", named + " has a superclass outside"
                    + " the jar, so this linter can’t tell whether it implements SacredMod."));
        } else if (!sacred) {
            found.add(Finding.error("entrypoint", named + " doesn’t implement"
                    + " dev.ancaria.coderpack.api.SacredMod, so the loader rejects it."));
        }
    }

    /** True, false, or null when the answer is outside the jar. */
    private static Boolean sacred(Mod mod, Klass klass) {
        boolean unknown = false;
        for (String name : klass.interfaces()) {
            if (SACRED_MOD.equals(name)) {
                return Boolean.TRUE;
            }
            Boolean above = above(mod, name);
            if (Boolean.TRUE.equals(above)) {
                return Boolean.TRUE;
            }
            unknown |= above == null;
        }
        String parent = klass.superName();
        if (parent != null && !"java/lang/Object".equals(parent)) {
            Boolean above = above(mod, parent);
            if (Boolean.TRUE.equals(above)) {
                return Boolean.TRUE;
            }
            unknown |= above == null;
        }
        return unknown ? null : Boolean.FALSE;
    }

    private static Boolean above(Mod mod, String name) {
        Klass known = mod.classes().get(name);
        if (known == null || !known.parsed()) {
            // A supertype from a library the mod packs, or from the JDK. Neither
            // can implement SacredMod without the API, but saying so for certain
            // would mean reading a classpath this linter does not have.
            return name.startsWith("java/") ? Boolean.FALSE : null;
        }
        return sacred(mod, known);
    }
}
