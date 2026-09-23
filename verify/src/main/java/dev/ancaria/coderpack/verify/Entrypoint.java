package dev.ancaria.coderpack.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The class the descriptor names: it is in the jar, the loader can build one,
 * and what it builds is a mod.
 *
 * <p>The loader does {@code loadClass(entrypoint)}, checks that the result is a
 * subclass of the abstract class {@code SacredMod}, and creates it through
 * {@code getDeclaredConstructor()}. Each error here is one of the ways that
 * ends in a stack trace in a player log instead of a loaded mod.
 *
 * <p>Visibility is a warning, not an error. The loader makes the constructor
 * accessible before it calls it, so a package-private class or constructor
 * still loads. The contract is a public class with a public no-argument
 * constructor all the same, and a mod should not lean on the loader's leniency.
 */
final class Entrypoint implements Check {

    private static final String SACRED_MOD = "dev/ancaria/coderpack/api/SacredMod";

    /** Where a walk up the superclass chain from the entrypoint ends. */
    private enum Lineage {
        /** Reaches the SacredMod class. */
        MOD,
        /** Implements SacredMod as the interface it was up to API 2. */
        LEGACY,
        /** Ends at Object, or in the JDK, without passing SacredMod. */
        NONE,
        /** Leaves the jar before either, so nothing can be said for certain. */
        UNKNOWN
    }

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
        if (!lineage(mod, named, klass, found)) {
            // Not a mod at all: whether it could be instantiated is beside the point.
            return;
        }
        if (klass.isAbstract()) {
            found.add(Finding.error("entrypoint", named + " is abstract and can’t be"
                    + " instantiated. Name a concrete subclass as the entrypoint."));
        }
        if (!klass.isPublic()) {
            found.add(Finding.warning("entrypoint", named + " isn’t public. Make it"
                    + " public: the loader is in another package, and a mod entrypoint"
                    + " is meant to be reachable from there."));
        }
        constructor(named, klass, found);
    }

    /** False when the class is no mod, which is already reported then. */
    private static boolean lineage(Mod mod, String named, Klass klass, List<Finding> found) {
        switch (walk(mod, klass)) {
            case MOD -> {
                return true;
            }
            case LEGACY -> found.add(Finding.error("entrypoint", named + " implements"
                    + " SacredMod as an interface, which it was up to API 2. Since API 3"
                    + " SacredMod is an abstract class and the loader can’t load this one."
                    + " Write “extends SacredMod”, override onLoad() without the Context"
                    + " parameter, and reach the context through getContext()."));
            case NONE -> found.add(Finding.error("entrypoint", named + " doesn’t extend"
                    + " dev.ancaria.coderpack.api.SacredMod, so the loader rejects it."));
            case UNKNOWN -> {
                found.add(Finding.warning("entrypoint", named + " has a superclass outside"
                        + " the jar, so this linter can’t tell whether it extends SacredMod."));
                return true;
            }
        }
        return false;
    }

    private static void constructor(String named, Klass klass, List<Finding> found) {
        Klass.Meth empty = klass.methods().stream()
                                .filter(method -> "<init>".equals(method.name()))
                                .filter(method -> "()V".equals(method.descriptor()))
                                .findFirst().orElse(null);
        if (empty == null) {
            found.add(Finding.error("entrypoint", named + " has no no-argument"
                    + " constructor. The loader calls getDeclaredConstructor() without"
                    + " arguments; the context comes from getContext(), not a parameter."));
        } else if (!empty.isPublic()) {
            found.add(Finding.warning("entrypoint", named + " has a non-public"
                    + " no-argument constructor. Make it public: the loader only reaches"
                    + " it by overriding the access check."));
        }
    }

    /**
     * Follows the superclass chain as far as the jar goes. A class on the way
     * that implements SacredMod, directly or through an interface of the jar,
     * settles it as {@link Lineage#LEGACY}: the JVM refuses to load a class
     * whose interface turned out to be a class.
     */
    private static Lineage walk(Mod mod, Klass start) {
        Set<String> seen = new HashSet<>();
        Klass klass = start;
        while (seen.add(klass.name())) {
            if (implementsLegacy(mod, klass, new HashSet<>())) {
                return Lineage.LEGACY;
            }
            String parent = klass.superName();
            if (SACRED_MOD.equals(parent)) {
                return Lineage.MOD;
            }
            if (parent == null || "java/lang/Object".equals(parent)) {
                return Lineage.NONE;
            }
            Klass known = mod.classes().get(parent);
            if (known == null || !known.parsed()) {
                // A superclass from a library the mod packs, or from the JDK.
                // Neither can extend SacredMod without the API, but saying so for
                // certain would mean reading a classpath this linter does not have.
                return parent.startsWith("java/") ? Lineage.NONE : Lineage.UNKNOWN;
            }
            klass = known;
        }
        // A cycle, which no compiler writes. Nothing sensible to add about it.
        return Lineage.UNKNOWN;
    }

    private static boolean implementsLegacy(Mod mod, Klass klass, Set<String> seen) {
        for (String name : klass.interfaces()) {
            if (SACRED_MOD.equals(name)) {
                return true;
            }
            Klass known = mod.classes().get(name);
            if (known != null && known.parsed() && seen.add(name)
                    && implementsLegacy(mod, known, seen)) {
                return true;
            }
        }
        return false;
    }
}
