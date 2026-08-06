package dev.ancaria.coderpack.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One jar per rule, each breaking that rule and nothing else.
 *
 * <p>Every case asserts the count as well as the message. A linter that finds
 * the right problem and three imaginary ones next to it is a linter people turn
 * off.
 */
class VerifierTest {

    private static final String DEMO = "demo/DemoMod.class";

    @TempDir
    Path directory;

    @Test
    void passes_a_mod_that_breaks_nothing() {
        Report report = verify(Jars.DECLARATION, DEMO);
        assertEquals(0, report.findings().size(), report.toString());
        assertTrue(report.ok());
    }

    @Test
    void finds_a_jar_with_no_descriptor() {
        Finding found = only(verify(null, DEMO));
        assertEquals(Level.ERROR, found.level());
        assertEquals("declaration", found.rule());
        assertTrue(found.detail().contains("No META-INF/declaration.toml"), found.detail());
    }

    @Test
    void finds_a_descriptor_with_no_entrypoint() {
        Finding found = only(verify("""
                id = "demo-mod"
                version = "1.0.0"
                api = "1"
                """, DEMO));
        assertTrue(found.detail().contains("No entrypoint"), found.detail());
    }

    @Test
    void finds_a_descriptor_with_no_api() {
        Finding found = only(verify("""
                id = "demo-mod"
                version = "1.0.0"
                entrypoint = "demo.DemoMod"
                """, DEMO));
        assertEquals(Level.ERROR, found.level());
        assertTrue(found.detail().contains("No “api” entry"), found.detail());
    }

    @Test
    void finds_a_mod_built_against_another_api() {
        Finding found = only(verify(Jars.DECLARATION.replace("api = \"1\"", "api = \"2\""),
                                    DEMO));
        assertTrue(found.detail().contains("api = “2”"), found.detail());
        assertTrue(found.detail().contains("API " + Verifier.API), found.detail());
    }

    /** A range covering this contract passes, however wide it is written. */
    @Test
    void accepts_a_range_that_covers_this_contract() {
        for (String range : new String[] {Verifier.API_RANGE, "[1,3)", "[1,)", "[1]", "1"}) {
            assertTrue(verify(Jars.DECLARATION.replace("api = \"1\"", "api = \"" + range + "\""),
                              DEMO).ok(), range);
        }
    }

    /** And one that does not is refused, with the range in the message. */
    @Test
    void refuses_a_range_this_toolchain_is_outside_of() {
        Finding found = only(verify(Jars.DECLARATION.replace("api = \"1\"", "api = \"[2,3)\""),
                                    DEMO));
        assertEquals(Level.ERROR, found.level());
        assertTrue(found.detail().contains("[2,3)"), found.detail());
    }

    @Test
    void finds_an_api_range_nothing_can_read() {
        Finding found = only(verify(Jars.DECLARATION.replace("api = \"1\"", "api = \"[1,2\""),
                                    DEMO));
        assertEquals(Level.ERROR, found.level());
        assertTrue(found.detail().contains("version range is invalid"), found.detail());
    }

    /**
     * The second range names launcher releases, which no toolchain can check --
     * the ones that matter have usually not happened yet. Syntax, and no more.
     */
    @Test
    void checks_the_loader_range_for_syntax_only() {
        assertTrue(verify(Jars.DECLARATION + "loader = \"[99.0.0,)\"\n", DEMO).ok());
        Finding found = only(verify(Jars.DECLARATION + "loader = \"2,3\"\n", DEMO));
        assertEquals(Level.ERROR, found.level());
        assertTrue(found.detail().contains("version range is invalid"), found.detail());
    }

    /**
     * The default the plugin stamps has to be one this linter accepts, or every
     * jar built with the toolchain fails its own check.
     */
    @Test
    void the_default_range_covers_the_contract_it_was_derived_from() {
        assertTrue(Verifier.builds(Verifier.API_RANGE), Verifier.API_RANGE);
    }

    @Test
    void finds_the_api_packed_into_the_jar() {
        Finding found = only(verify(Jars.DECLARATION, DEMO,
                                    "dev/ancaria/coderpack/api/SacredMod.class",
                                    "dev/ancaria/coderpack/api/Context.class"));
        assertEquals("api", found.rule());
        assertTrue(found.detail().contains("2 API classes"), found.detail());
    }

    @Test
    void finds_an_entrypoint_that_is_not_in_the_jar() {
        Finding found = only(verify(
                Jars.DECLARATION.replace("demo.DemoMod", "demo.Absent"), DEMO));
        assertEquals("entrypoint", found.rule());
        assertTrue(found.detail().contains("isn’t in the jar"), found.detail());
    }

    @Test
    void finds_an_entrypoint_that_is_not_a_mod() {
        Finding found = only(entry("demo.NotAMod", "demo/NotAMod.class"));
        assertTrue(found.detail().contains("doesn’t implement"), found.detail());
    }

    @Test
    void finds_an_entrypoint_that_cannot_be_instantiated() {
        Finding found = only(entry("demo.AbstractMod", "demo/AbstractMod.class"));
        assertTrue(found.detail().contains("is abstract"), found.detail());
    }

    @Test
    void finds_an_entrypoint_with_no_no_argument_constructor() {
        Finding found = only(entry("demo.NoCtor", "demo/NoCtor.class"));
        assertTrue(found.detail().contains("no no-argument constructor"), found.detail());
    }

    @Test
    void finds_an_entrypoint_the_loader_cannot_reach() {
        Finding found = only(entry("demo.HiddenMod", "demo/HiddenMod.class"));
        assertTrue(found.detail().contains("isn’t public"), found.detail());
    }

    @Test
    void finds_a_listener_with_the_wrong_number_of_parameters() {
        Finding found = only(verify(Jars.DECLARATION, DEMO, "demo/Arity.class"));
        assertEquals("listener", found.rule());
        assertTrue(found.detail().contains("takes 2 parameters"), found.detail());
    }

    @Test
    void finds_a_listener_that_does_not_take_an_event() {
        Finding found = only(verify(Jars.DECLARATION, DEMO, "demo/NotEvent.class"));
        assertTrue(found.detail().contains("is not an event"), found.detail());
    }

    @Test
    void finds_a_listener_that_returns_something() {
        Finding found = only(verify(Jars.DECLARATION, DEMO, "demo/Returns.class"));
        assertTrue(found.detail().contains("returns java.lang.String"), found.detail());
    }

    @Test
    void warns_about_a_listener_the_bus_never_sees() {
        Report report = verify(Jars.DECLARATION, DEMO, "demo/Quiet.class");
        Finding found = only(report);
        assertEquals(Level.WARNING, found.level());
        assertTrue(found.detail().contains("not public"), found.detail());
        // A warning says something is pointless, never that it is broken, so the
        // jar still passes.
        assertTrue(report.ok());
    }

    @Test
    void finds_a_signature_file_from_another_jar() {
        Finding found = only(verify(Jars.DECLARATION, DEMO, "META-INF/SOMEBODY.SF"));
        assertEquals("signature", found.rule());
        assertTrue(found.detail().contains("signature file"), found.detail());
    }

    @Test
    void says_so_when_the_file_is_not_a_jar() throws IOException {
        Path text = Files.writeString(directory.resolve("mod.jar"), "not a zip");
        Finding found = only(Verifier.verify(text));
        assertEquals("jar", found.rule());
    }

    /** A jar whose only class is the entrypoint the descriptor names. */
    private Report entry(String entrypoint, String klass) {
        return verify(Jars.DECLARATION.replace("demo.DemoMod", entrypoint), klass);
    }

    private Report verify(String declaration, String... entries) {
        return Verifier.verify(Jars.build(directory, "demo-mod.jar", declaration, entries));
    }

    private static Finding only(Report report) {
        assertEquals(1, report.findings().size(), report.toString());
        return report.findings().getFirst();
    }
}
