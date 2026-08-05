package dev.ancaria.coderpack.verify;

import org.objectweb.asm.Type;

import java.util.List;

/**
 * Every {@code @Subscribe} method: one parameter, that parameter an event, and
 * no return value.
 *
 * <p>The bus checks the same thing when a mod registers, writes a line into the
 * log and moves on. That line arrives while a player is starting the game, and
 * what they see is a mod that loaded and does nothing. The shape is decided at
 * compile time, so it belongs in a build.
 */
final class Listeners implements Check {

    private static final String EVENTS = "dev/ancaria/coderpack/api/event/";

    /** The one thing in the event package that is not an event. */
    private static final String GUARD = EVENTS + "Guard";

    @Override
    public void run(Mod mod, List<Finding> found) {
        for (Klass klass : sorted(mod)) {
            for (Klass.Meth method : klass.methods()) {
                if (method.subscribes()) {
                    check(mod, klass.dotted() + "." + method.name(), method, found);
                }
            }
        }
    }

    private static void check(Mod mod, String where, Klass.Meth method,
                              List<Finding> found) {
        Type[] parameters = Type.getArgumentTypes(method.descriptor());
        if (parameters.length != 1) {
            found.add(Finding.error("listener", where + " takes " + parameters.length
                    + " parameters; the bus registers a listener with exactly one, and"
                    + " the parameter is what it subscribes to"));
        } else if (!isEvent(mod, parameters[0])) {
            found.add(Finding.error("listener", where + " takes "
                    + parameters[0].getClassName() + ", which is not an event; the bus"
                    + " skips it and this listener never fires"));
        }
        Type returned = Type.getReturnType(method.descriptor());
        if (returned.getSort() != Type.VOID) {
            found.add(Finding.error("listener", where + " returns "
                    + returned.getClassName() + "; a listener returns void, and whatever"
                    + " it hands back is thrown away"));
        }
        if (!method.isPublic()) {
            found.add(Finding.warning("listener", where + " is not public; the bus reads"
                    + " public methods only, so this one never fires"));
        }
    }

    /** Everything in the API event package is an event, bar the loader handle. */
    private static boolean isEvent(Mod mod, Type parameter) {
        if (parameter.getSort() != Type.OBJECT) {
            return false;
        }
        String name = parameter.getInternalName();
        if (name.startsWith(EVENTS)) {
            return !name.equals(GUARD);
        }
        Klass known = mod.classes().get(name);
        return known != null && known.parsed() && known.superName() != null
                && isEvent(mod, Type.getObjectType(known.superName()));
    }

    /** By name, so two runs over one jar print their findings in one order. */
    private static List<Klass> sorted(Mod mod) {
        return mod.classes().values().stream()
                  .filter(klass -> !klass.methods().isEmpty())
                  .sorted((left, right) -> left.name().compareTo(right.name()))
                  .toList();
    }
}
