package dev.ancaria.coderpack.verify;

import java.util.regex.Pattern;

/**
 * What a mod is allowed to call itself.
 *
 * <p>The launcher writes the id into its enabled list and the loader keys its
 * mod table on it, so it has to survive a file, a config line and a command
 * line unchanged: lowercase letters, digits and inner hyphens.
 *
 * <p>Three things ask this question -- the Gradle plugin before it writes a
 * descriptor, this linter after somebody else wrote one, and the scaffolder
 * before it writes a project. They ask it here so a fourth answer cannot appear.
 */
public final class Ids {

    /** The rule, spelled out for a message that has to show it. */
    public static final String PATTERN = "[a-z0-9]([a-z0-9-]*[a-z0-9])?";

    private static final Pattern ID = Pattern.compile(PATTERN);

    private Ids() {
    }

    public static boolean valid(String id) {
        return id != null && ID.matcher(id).matches();
    }
}
