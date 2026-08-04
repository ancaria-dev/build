package dev.ancaria.coderpack.verify;

import java.util.Locale;

/**
 * How much a finding matters.
 *
 * <p>Only an {@link #ERROR} fails a build. A {@link #WARNING} is a jar that
 * loads and then does less than its author meant it to -- a listener nothing
 * ever calls, a class file newer than the JVM a player is likely to have. The
 * split exists so this linter can say those things without ever refusing a jar
 * that would have worked.
 */
public enum Level {

    ERROR,
    WARNING;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
