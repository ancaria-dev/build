package dev.ancaria.coderpack.verify;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Opens a mod jar and reads it into a {@link Mod}. One pass, nothing loaded. */
final class Jar {

    private Jar() {
    }

    static Mod read(Path path) throws IOException {
        List<String> entries = new ArrayList<>();
        Map<String, Klass> classes = new HashMap<>();
        String declaration = null;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            for (Enumeration<? extends ZipEntry> all = zip.entries(); all.hasMoreElements(); ) {
                ZipEntry entry = all.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                entries.add(name);
                if (name.equals(Mod.DESCRIPTOR)) {
                    declaration = new String(bytes(zip, entry), StandardCharsets.UTF_8);
                } else if (name.endsWith(".class")) {
                    Klass klass = Klass.read(name, bytes(zip, entry));
                    classes.put(klass.name(), klass);
                }
            }
        }
        Map<String, String> values = declaration == null ? Map.of() : Toml.parse(declaration);
        return new Mod(path, List.copyOf(entries), Map.copyOf(classes), declaration, values);
    }

    /**
     * The descriptor alone, for a caller that wants what a jar says about itself
     * and not what is wrong with it. Null when there is none.
     *
     * <p>A separate pass rather than {@link #read}, because that one runs every
     * class in the jar through ASM and a mod that packs a UI toolkit is nine
     * megabytes of classes nobody asked about.
     */
    static String declaration(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry entry = zip.getEntry(Mod.DESCRIPTOR);
            return entry == null
                    ? null
                    : new String(bytes(zip, entry), StandardCharsets.UTF_8);
        }
    }

    private static byte[] bytes(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }
}
