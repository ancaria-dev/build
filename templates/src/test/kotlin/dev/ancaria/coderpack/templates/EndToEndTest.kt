package dev.ancaria.coderpack.templates

import dev.ancaria.coderpack.verify.Verifier
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createTempDirectory
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The test that matters: scaffold a mod, build it with a real Gradle, and read
 * the jar that comes out with the linter.
 *
 * Every unit test above can pass while the generated project does not resolve,
 * does not compile, or packs something the loader refuses -- and that project is
 * the whole product. The plugin comes out of the local Maven repository, which
 * is how a mod author gets it, so `:plugin` and `:verify` are published there
 * before this runs.
 *
 * Once per language, because a language is exactly the part of a template that
 * a unit test cannot check: whether the compiler was told to emit Java 21, and
 * whether the runtime it needs is inside the jar rather than merely on the
 * compile classpath. `findings()` being empty covers both -- the linter warns on
 * a class file above 65 and errors on an entrypoint it cannot resolve.
 *
 * And once per build DSL on top of that, for the same reason one level down: a
 * `build.gradle` is a second file saying the same thing in a syntax nothing
 * here type-checks, and the way to find out whether it says it correctly is to
 * hand it to Gradle. Six cases, and the Kotlin and Groovy ones are slow.
 */
class EndToEndTest {

    private val directory: Path = createTempDirectory("coderpack-e2e")

    @AfterTest
    fun cleanUp() {
        directory.toFile().deleteRecursively()
    }

    @Test
    fun `a scaffolded Java mod builds and passes the linter`() = buildsClean("java", "kotlin")

    @Test
    fun `a scaffolded Kotlin mod builds and passes the linter`() = buildsClean("kotlin", "kotlin")

    @Test
    fun `a scaffolded Groovy mod builds and passes the linter`() = buildsClean("groovy", "kotlin")

    @Test
    fun `a Java mod built by a Groovy DSL script builds too`() = buildsClean("java", "groovy")

    @Test
    fun `a Kotlin mod built by a Groovy DSL script builds too`() = buildsClean("kotlin", "groovy")

    @Test
    fun `a Groovy mod built by a Groovy DSL script builds too`() = buildsClean("groovy", "groovy")

    private fun buildsClean(language: String, dsl: String) {
        val project = directory.resolve("$language-$dsl")
        New.run(
            listOf(
                "demo-mod",
                "--dir", project.toString(),
                "--language", language,
                "--dsl", dsl,
                "--description", "Proves the scaffolder writes something that builds",
                "--author", "MairwunNx (Pavel Erokhin)",
            )
        )
        api(project, dsl)

        val result = GradleRunner.create()
            .withProjectDir(project.toFile())
            .withArguments("assembleSacredMod", "--configuration-cache", "--stacktrace")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":assembleSacredMod")?.outcome, result.output)
        // Packaging runs the linter, so this jar has already passed it once.
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySacredMod")?.outcome, result.output)

        val jar = project.resolve("build/sacred-mod/demo-mod-1.0.0.jar")
        assertTrue(jar.isRegularFile(), "no jar at $jar")

        // And again from here, on the file itself, the way `coderpack verify`
        // does: nothing at all, not even a warning.
        val report = Verifier.verify(jar)
        assertEquals(emptyList(), report.findings(), report.toString())

        // And the last step of the same story: the project describes itself to a
        // launcher without anybody writing json. The index is generated from the
        // jar above, so this fails if the two ever stop agreeing.
        assertEquals(0, Registry.run(listOf(project.toString())))
        val index = project.resolve(Registry.INDEX).readText()
        assertTrue(""""id": "demo-mod"""" in index, index)
        assertTrue(""""version": "1.0.0"""" in index, index)
        assertTrue(""""file": "demo-mod-1.0.0.jar"""" in index, index)
        assertTrue(sha256(jar) in index, index)
    }

    /** What the index claims about the jar, worked out the other way round. */
    private fun sha256(jar: Path): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(jar.toFile().readBytes())
            .joinToString("") { "%02x".format(it) }

    /**
     * Offers the linter's stub of the loader API under the real coordinates.
     *
     * `dev.ancaria.coderpack:api` is published by coderpack, which this
     * repository does not read and CI does not check out. This is the one line
     * of the generated project the test changes, and it changes where a
     * dependency comes from rather than what the project is.
     */
    private fun api(project: Path, dsl: String) {
        val stub = Path.of(System.getProperty("coderpack.apiStub")).invariantSeparatorsPathString
        // The one difference between the two syntaxes that this test has to
        // know about, and it is a pair of brackets.
        val dirs = if (dsl == "groovy") """dirs "$stub"""" else """dirs("$stub")"""
        project.resolve(Dsls.values(dsl).getValue("settingsFile")).appendText(
            """

            dependencyResolutionManagement {
                repositories {
                    flatDir { $dirs }
                }
            }
            """.trimIndent() + "\n"
        )
    }
}
