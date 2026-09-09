package dev.ancaria.coderpack.plugin

import dev.ancaria.coderpack.verify.Verifier
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Builds a real mod with a real Gradle. Unit tests over the extension would pass
 * while the produced jar was unloadable, which is the only thing that matters
 * here.
 */
class SacredPluginTest {

    private val projectDir: File = createTempDirectory("sacred-mod").toFile()

    @AfterTest
    fun cleanUp() {
        projectDir.deleteRecursively()
    }

    @Test
    fun `packs a mod with its descriptor and its dependencies`() {
        write("settings.gradle.kts", """
            dependencyResolutionManagement { repositories { mavenCentral() } }
            rootProject.name = "demo"
        """)
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }

            version = "1.2.3"

            // The loader hands a mod the API, so a mod compiles against it and
            // does not pack it. Stubs in a source set of their own rather than
            // the published artifact: this test needs no credentials for a
            // package registry, and compileOnly keeps them out of the jar --
            // which is one of the things the linter is about to check.
            val loaderApi = sourceSets.create("loaderApi")

            dependencies {
                compileOnly(loaderApi.output)
                // A tiny real dependency: the point of the fat jar is that this
                // ends up inside it.
                implementation("org.jetbrains:annotations:26.0.2")
            }

            sacred {
                id = "demo-mod"
                displayName = "Demo Mod"
                description = "Proves the plugin works"
                entrypoint = "demo.DemoMod"
                authors = listOf("MairwunNx (Pavel Erokhin)", "Somebody Else")
                website = "https://ancaria.dev"
                repository = "https://github.com/ancaria-dev/mods"
                conflictsWith("other-demo-mod")
            }
        """)
        stubApi()
        write("src/main/java/demo/DemoMod.java", """
            package demo;

            import dev.ancaria.coderpack.api.Context;
            import dev.ancaria.coderpack.api.SacredMod;

            public final class DemoMod implements SacredMod {
                public static final String NAME = "demo";

                @Override
                public void onLoad(Context context) {
                }
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("assembleSacredMod", "--configuration-cache", "--stacktrace")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":assembleSacredMod")?.outcome)
        // Packaging runs the linter, so a jar reaching build/sacred-mod is a jar
        // the loader would take.
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySacredMod")?.outcome)

        val jar = File(projectDir, "build/sacred-mod/demo-mod-1.2.3.jar")
        assertTrue(jar.isFile, "expected a jar at ${jar.path}")

        ZipFile(jar).use { zip ->
            val descriptor = zip.getEntry(Descriptor.DESCRIPTOR)
            assertTrue(descriptor != null, "the jar has no descriptor")
            val text = zip.getInputStream(descriptor).readBytes().decodeToString()
            assertTrue("""id = "demo-mod"""" in text, text)
            assertTrue("""name = "Demo Mod"""" in text, text)
            assertTrue("""version = "1.2.3"""" in text, text)
            assertTrue("""entrypoint = "demo.DemoMod"""" in text, text)
            // Nobody writes this one by hand, and a jar without it is a jar the
            // loader refuses.
            assertTrue("""api = "${Descriptor.API}"""" in text, text)
            assertTrue("""authors = ["MairwunNx (Pavel Erokhin)", "Somebody Else"]""" in text, text)
            assertTrue("""website = "https://ancaria.dev"""" in text, text)
            // The launcher hides both sides of a conflict, and it reads this
            // line to know there is one.
            assertTrue("""conflicts = ["other-demo-mod"]""" in text, text)

            assertTrue(zip.getEntry("demo/DemoMod.class") != null, "the mod's own class is missing")
            assertTrue(
                zip.getEntry("org/jetbrains/annotations/NotNull.class") != null,
                "the dependency is not inside the jar, so the loader could not find it"
            )
            assertTrue(
                zip.getEntry("dev/ancaria/coderpack/api/SacredMod.class") == null,
                "the API is inside the jar, which is the one thing a mod must not do"
            )
        }
    }

    @Test
    fun `refuses to pack a mod the loader would skip`() {
        write("settings.gradle.kts", """rootProject.name = "demo"""")
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }
            sacred {
                id = "demo-mod"
                // Nothing in the jar is called this.
                entrypoint = "demo.Absent"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("assembleSacredMod", "--configuration-cache")
            .buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":verifySacredMod")?.outcome)
        assertTrue("demo.Absent isn’t in the jar" in result.output, result.output)
        // The lint runs before the copy, so nothing was produced to install.
        assertTrue(
            !File(projectDir, "build/sacred-mod").isDirectory,
            "a jar that fails the lint was copied anyway"
        )
    }

    @Test
    fun `refuses an id the launcher could not write down`() {
        write("settings.gradle.kts", """rootProject.name = "demo"""")
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }
            sacred {
                id = "Not An Id"
                entrypoint = "demo.DemoMod"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateModDescriptor")
            .buildAndFail()

        assertTrue("lowercase letters, digits" in result.output, result.output)
    }

    /**
     * A mod that spans two majors, and one that names a launcher release. Both
     * are the author's words, and an extension property that is not wired into
     * the `@CacheableTask` is silently ignored, so the check is the descriptor
     * on disk, not the extension.
     */
    @Test
    fun `writes the ranges a mod author asked for`() {
        write("settings.gradle.kts", """rootProject.name = "demo"""")
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }
            sacred {
                id = "demo-mod"
                entrypoint = "demo.DemoMod"
                apiRange = "[${Verifier.API},99)"
                loaderRange = "[0.1.20,)"
            }
        """)

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateModDescriptor", "--configuration-cache")
            .build()

        val text = File(projectDir, "build/generated/sacred-mod/${Descriptor.DESCRIPTOR}").readText()
        assertTrue("""api = "[${Verifier.API},99)"""" in text, text)
        assertTrue("""loader = "[0.1.20,)"""" in text, text)
    }

    /** And nothing at all when the author says nothing, which is most mods. */
    @Test
    fun `writes no loader line when none was asked for`() {
        write("settings.gradle.kts", """rootProject.name = "demo"""")
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }
            sacred {
                id = "demo-mod"
                entrypoint = "demo.DemoMod"
            }
        """)

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateModDescriptor", "--configuration-cache")
            .build()

        val text = File(projectDir, "build/generated/sacred-mod/${Descriptor.DESCRIPTOR}").readText()
        assertTrue("""api = "${Descriptor.API}"""" in text, text)
        assertTrue("loader" !in text, text)
    }

    /**
     * The one thing an author may not do. The loader believes the descriptor, so
     * a range claiming a contract nothing here compiled against is a promise
     * nobody checked, and it fails on the line they typed rather than on the
     * jar three tasks later.
     */
    @Test
    fun `refuses a range this toolchain did not build`() {
        write("settings.gradle.kts", """rootProject.name = "demo"""")
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }
            sacred {
                id = "demo-mod"
                entrypoint = "demo.DemoMod"
                apiRange = "[99,100)"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateModDescriptor")
            .buildAndFail()

        assertTrue("sacred.apiRange is “[99,100)”" in result.output, result.output)
    }

    @Test
    fun `refuses a range nothing can read`() {
        write("settings.gradle.kts", """rootProject.name = "demo"""")
        write("build.gradle.kts", """
            plugins { id("dev.ancaria.coderpack") }
            sacred {
                id = "demo-mod"
                entrypoint = "demo.DemoMod"
                loaderRange = "2,3"
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateModDescriptor")
            .buildAndFail()

        assertTrue("sacred.loaderRange isn’t a valid version range" in result.output, result.output)
    }

    /** Just enough of the API for a mod to be one. The loader provides the rest. */
    private fun stubApi() {
        write("src/loaderApi/java/dev/ancaria/coderpack/api/Context.java", """
            package dev.ancaria.coderpack.api;
            public interface Context {
            }
        """)
        write("src/loaderApi/java/dev/ancaria/coderpack/api/SacredMod.java", """
            package dev.ancaria.coderpack.api;
            public interface SacredMod {
                void onLoad(Context context);
            }
        """)
    }

    private fun write(path: String, content: String) {
        val file = File(projectDir, path)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent())
    }
}
