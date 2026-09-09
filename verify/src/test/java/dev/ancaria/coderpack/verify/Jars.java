package dev.ancaria.coderpack.verify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a mod jar out of the fixture class files.
 *
 * <p>Real jars, because that is what the linter is pointed at. The class files
 * come from the {@code fixtures} source set, compiled by javac like any other
 * code in this build, so a test never has to describe bytecode it wants.
 */
final class Jars {

    /** Written by the build; see the test task in build.gradle.kts. */
    private static final Path CLASSES = Path.of(System.getProperty("verify.fixtures"));

    /** What the plugin writes for the fixture mod, and what every case starts from. */
    static final String DECLARATION = """
            id = "demo-mod"
            name = "Demo Mod"
            version = "1.0.0"
            entrypoint = "demo.DemoMod"
            api = "1"
            """;

    private Jars() {
    }

    /**
     * @param declaration the descriptor, or null for a jar without one
     * @param entries     paths inside the jar. One ending in {@code .class} is
     *                    copied from the fixtures. Anything else is written
     *                    empty, which is all a signature file has to be.
     */
    static Path build(Path directory, String name, String declaration, String... entries) {
        Path jar = directory.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            if (declaration != null) {
                write(zip, Mod.DESCRIPTOR, declaration.getBytes(StandardCharsets.UTF_8));
            }
            for (String entry : entries) {
                write(zip, entry, entry.endsWith(".class")
                        ? Files.readAllBytes(CLASSES.resolve(entry)) : new byte[0]);
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return jar;
    }

    private static void write(ZipOutputStream zip, String name, byte[] bytes)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
