package dev.ancaria.coderpack.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The descriptor, read exactly the way the loader reads it.
 *
 * <p>{@code key = "value"} and nothing else. That is the same subset as
 * {@code Meta.parse} in the zygote and {@code parse} in the launcher, on
 * purpose: a linter that understood more TOML than they do would pass a
 * descriptor neither of them can read.
 */
final class Toml {

    private Toml() {
    }

    /**
     * A {@code ["a", "b"]} value split back into the strings inside it.
     *
     * <p>Separate from {@link #parse} because a value stays in the map the way
     * it was written. Whoever wants a list asks for one, and everything else
     * goes on seeing a single string, which is what both readers do with
     * {@code authors} today.
     *
     * <p>Anything that is not bracketed counts as one item. A descriptor is
     * written by the plugin, and a hand-made one saying
     * {@code conflicts = "other-mod"} means something obvious enough to honour.
     */
    static List<String> list(String value) {
        List<String> items = new ArrayList<>();
        if (value == null) {
            return items;
        }
        String body = value.strip();
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        for (String part : body.split(",")) {
            String item = part.strip();
            if (item.length() >= 2 && item.startsWith("\"") && item.endsWith("\"")) {
                item = item.substring(1, item.length() - 1);
            }
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    static Map<String, String> parse(String text) {
        Map<String, String> values = new HashMap<>();
        for (String line : text.lines().toList()) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int split = trimmed.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String key = trimmed.substring(0, split).strip();
            String value = trimmed.substring(split + 1).strip();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }
}
