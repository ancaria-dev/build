package dev.ancaria.coderpack.verify;

import java.nio.file.Path;
import java.util.List;

/**
 * What one jar came back with.
 *
 * <p>{@link #toString()} is the printed form, and both callers use it: the
 * Gradle task and the command line say the same thing about the same jar.
 */
public record Report(Path jar, List<Finding> findings) {

    /** True when nothing here stops the jar from loading. Warnings do not. */
    public boolean ok() {
        return count(Level.ERROR) == 0;
    }

    public long count(Level level) {
        return findings.stream().filter(finding -> finding.level() == level).count();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(jar.getFileName() + ": " + summary());
        for (Finding finding : findings) {
            text.append("\n  ").append(finding);
        }
        return text.toString();
    }

    private String summary() {
        long errors = count(Level.ERROR);
        long warnings = count(Level.WARNING);
        if (errors == 0 && warnings == 0) {
            return "ok";
        }
        String head = errors == 0 ? "ok" : plural(errors, "error");
        return warnings == 0 ? head : head + ", " + plural(warnings, "warning");
    }

    private static String plural(long amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }
}
