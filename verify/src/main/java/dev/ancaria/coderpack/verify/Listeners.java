package dev.ancaria.coderpack.verify;

import org.objectweb.asm.Type;

import java.util.List;
import java.util.Set;

/**
 * Every {@code @Subscribe} method: one parameter, that parameter an event, and
 * a return type that says what the method may do.
 *
 * <p>{@code void} observes. Anything else must be that event's own nested
 * {@code Mutation}, which only a decidable event has, so a listener on an event
 * with nothing to decide cannot claim to decide anything. A {@code MONITOR}
 * method watches, so it returns {@code void} too.
 *
 * <p>The bus checks the same things when a mod registers, writes a line into
 * the log and moves on. That line arrives while a player is starting the game,
 * and what they see is a mod that loaded and does nothing. The shape is decided
 * at compile time, so it belongs in a build.
 */
final class Listeners implements Check {

    private static final String EVENTS = "dev/ancaria/coderpack/api/event/";

    /** What lives in the event package without being an event. */
    private static final Set<String> NOT_EVENTS = Set.of(
            EVENTS + "EventMutation", EVENTS + "Decides", EVENTS + "Fold",
            EVENTS + "Delivery");

    private static final String MONITOR = "MONITOR";

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
        Type event = null;
        if (parameters.length != 1) {
            found.add(Finding.error("listener", where + " takes " + parameters.length
                    + " parameters. The bus registers a listener with exactly one, and"
                    + " the parameter is what it subscribes to"));
        } else if (!isEvent(mod, parameters[0])) {
            found.add(Finding.error("listener", where + " takes "
                    + parameters[0].getClassName() + ", which is not an event; the bus"
                    + " skips it and this listener never fires"));
        } else {
            event = parameters[0];
        }
        returns(where, method, event, found);
        if (!method.isPublic()) {
            found.add(Finding.warning("listener", where + " is not public; the bus reads"
                    + " public methods only, so this one never fires"));
        }
    }

    /**
     * The return is the permission. A listener that decides hands back that
     * event's own mutation and nothing else: another event's would compile, and
     * the loader would fold a gold answer into a damage decision.
     */
    private static void returns(String where, Klass.Meth method, Type event,
                                List<Finding> found) {
        Type returned = Type.getReturnType(method.descriptor());
        if (returned.getSort() == Type.VOID) {
            return;
        }
        if (MONITOR.equals(method.priority())) {
            found.add(Finding.error("listener", where + " is MONITOR and returns "
                    + dotted(returned) + ". A monitor watches: the loader throws"
                    + " its answer away, so this decides nothing"));
            return;
        }
        if (event == null) {
            return;
        }
        String wanted = event.getInternalName() + "$Mutation";
        if (!wanted.equals(returned.getInternalName())) {
            found.add(Finding.error("listener", where + " returns "
                    + dotted(returned) + "; a listener on "
                    + event.getClassName() + " returns void or "
                    + wanted.replace('/', '.').replace('$', '.')));
        }
    }

    /** A nested class reads as Gold.Mutation here, not Gold$Mutation. */
    private static String dotted(Type type) {
        return type.getClassName().replace('$', '.');
    }

    /** Everything in the API event package is an event, bar the few that are not. */
    private static boolean isEvent(Mod mod, Type parameter) {
        if (parameter.getSort() != Type.OBJECT) {
            return false;
        }
        String name = parameter.getInternalName();
        if (name.startsWith(EVENTS)) {
            // A nested type there is a Mutation or the shape one shares, never
            // an event.
            return !NOT_EVENTS.contains(name) && name.indexOf('$') < 0;
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
