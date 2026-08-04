package dev.ancaria.coderpack.verify;

/**
 * One thing wrong with one jar.
 *
 * <p>{@code rule} is the short name of what was being checked and {@code detail}
 * is written for whoever has to fix it: what is wrong, and what the loader does
 * about it.
 */
public record Finding(Level level, String rule, String detail) {

    static Finding error(String rule, String detail) {
        return new Finding(Level.ERROR, rule, detail);
    }

    static Finding warning(String rule, String detail) {
        return new Finding(Level.WARNING, rule, detail);
    }

    @Override
    public String toString() {
        return String.format("%-7s %-11s %s", level, rule, detail);
    }
}
