package dev.ancaria.coderpack.templates

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The command as somebody types it, minus the second Gradle. */
class NewTest {

    private val directory: Path = createTempDirectory("coderpack-new")

    @AfterTest
    fun cleanUp() {
        directory.toFile().deleteRecursively()
    }

    @Test
    fun `writes a project that says what it was told`() {
        val target = new(
            "my-mod",
            "--package", "dev.example.mymod",
            "--display-name", "My Splendid Mod",
            "--author", "Somebody",
        )

        val build = target.resolve("build.gradle.kts").readText()
        assertTrue("""id = "my-mod"""" in build, build)
        assertTrue("""displayName = "My Splendid Mod"""" in build, build)
        assertTrue("""entrypoint = "dev.example.mymod.MyMod"""" in build, build)
        assertTrue("""author("Somebody")""" in build, build)
        // Not typed in a template: the tool cannot name a version nobody built.
        assertTrue("""version "${Versions.plugin}"""" in build, build)
        assertTrue("""apiVersion = "${Versions.api}"""" in build, build)

        assertTrue(target.resolve("src/main/java/dev/example/mymod/MyMod.java").exists())
        // A copy skips a dotfile, so the template spells it _gitignore and the
        // project gets the dot. Worth a test: nothing else would notice.
        assertTrue(target.resolve(".gitignore").exists())
        // One command has to be enough, which means the project brings a Gradle.
        assertTrue(Files.size(target.resolve("gradle/wrapper/gradle-wrapper.jar")) > 0)
        assertTrue(target.resolve("gradlew").exists() && target.resolve("gradlew.bat").exists())
    }

    @Test
    fun `writes Java when nobody says otherwise`() {
        val target = new("plain")
        assertTrue(target.resolve("src/main/java/mods/plain/Plain.java").exists())
        val build = target.resolve("build.gradle.kts").readText()
        // No language plugin at all: `java` is the one the mod plugin applies.
        assertFalse("kotlin(" in build, build)
        assertFalse("groovy" in build, build)
    }

    @Test
    fun `writes the language it was asked for, and its build script with it`() {
        val kotlin = new("kotlin-mod", "--language", "kotlin")
        assertTrue(kotlin.resolve("src/main/kotlin/mods/kotlinmod/KotlinMod.kt").exists())
        val kotlinBuild = kotlin.resolve("build.gradle.kts").readText()
        assertTrue("""kotlin("jvm") version""" in kotlinBuild, kotlinBuild)
        // implementation, not compileOnly: the loader hands a mod the API and
        // nothing else, so the standard library has to be inside the jar.
        assertTrue("""implementation(kotlin("stdlib"))""" in kotlinBuild, kotlinBuild)
        // And a target, or the mod is compiled for whatever JDK is on the path.
        assertTrue("JvmTarget.JVM_21" in kotlinBuild, kotlinBuild)

        val groovy = new("groovy-mod", "--language", "groovy")
        assertTrue(groovy.resolve("src/main/groovy/mods/groovymod/GroovyMod.groovy").exists())
        val groovyBuild = groovy.resolve("build.gradle.kts").readText()
        assertTrue("""implementation("org.apache.groovy:groovy:""" in groovyBuild, groovyBuild)
        assertTrue("""targetCompatibility = "21"""" in groovyBuild, groovyBuild)

        // The build script is Kotlin DSL whichever language the mod is in.
        assertFalse(groovy.resolve("build.gradle").exists())
    }

    /**
     * The build is typed in the syntax that was asked for, and the mod is not.
     *
     * Two independent questions with the same three answers between them, which
     * is exactly why they are worth one test: `--dsl groovy` on a Java mod has
     * to give a `build.gradle` with no Groovy plugin in it, and a Kotlin mod
     * with `--dsl groovy` has to apply the Kotlin plugin from a `build.gradle`.
     */
    @Test
    fun `writes the build script in the DSL it was asked for`() {
        val java = new("groovy-built", "--dsl", "groovy")
        assertFalse(java.resolve("build.gradle.kts").exists())
        assertFalse(java.resolve("settings.gradle.kts").exists())
        assertTrue(java.resolve("settings.gradle").exists())
        // Java is still the mod's language: no language plugin at all.
        val plain = java.resolve("build.gradle").readText()
        assertTrue("""id 'dev.ancaria.coderpack' version""" in plain, plain)
        assertFalse("org.jetbrains.kotlin" in plain, plain)
        assertTrue(java.resolve("src/main/java/mods/groovybuilt/GroovyBuilt.java").exists())

        val kotlin = new("kotlin-in-groovy", "--dsl", "groovy", "--language", "kotlin")
        val script = kotlin.resolve("build.gradle").readText()
        assertTrue("""id 'org.jetbrains.kotlin.jvm' version""" in script, script)
        assertTrue("kotlin-stdlib" in script, script)
        assertTrue("JvmTarget.JVM_21" in script, script)
        assertTrue(kotlin.resolve("src/main/kotlin/mods/kotliningroovy/KotlinInGroovy.kt").exists())

        assertFailsWith<Fail> { new("wrong-dsl", "--dsl", "make") }
    }

    /**
     * A mod on its own, with nobody to install it from.
     *
     * The default is the other way round on purpose -- a mod nobody can install
     * is a mod nobody has -- so this is the flag, and what it drops is the SRML
     * half and nothing else.
     */
    @Test
    fun `writes a mod and no repository when told not to`() {
        val target = new("private-mod", "--no-registry")
        assertFalse(target.resolve("registry.toml").exists())
        assertFalse(target.resolve(".github/workflows/build.yml").exists())
        assertFalse(target.resolve("dependencies.json").exists())
        // Still a mod, and still one that builds.
        assertTrue(target.resolve("build.gradle.kts").exists())
        assertTrue(target.resolve("src/main/java/mods/privatemod/PrivateMod.java").exists())
        assertTrue(target.resolve("gradlew").exists())
    }

    @Test
    fun `refuses a language before it writes anything`() {
        val target = directory.resolve("rusty")
        val failed = assertFailsWith<Fail> {
            New.run(listOf("rusty", "--dir", target.toString(), "--language", "rust"))
        }
        assertTrue("rust" in failed.message!!, failed.message!!)
        assertTrue("kotlin" in failed.message!!, failed.message!!)
        assertFalse(target.exists(), "a refused language wrote a directory anyway")
    }

    @Test
    fun `leaves nothing behind when the id is refused`() {
        val target = directory.resolve("Not An Id")
        assertFailsWith<Fail> {
            New.run(listOf("Not An Id", "--dir", target.toString()))
        }
        assertFalse(target.exists(), "a refused name wrote a directory anyway")
    }

    @Test
    fun `refuses a directory somebody is already using`() {
        val target = directory.resolve("taken")
        Files.createDirectories(target)
        target.resolve("notes.txt").writeText("mine")

        val failed = assertFailsWith<Fail> {
            New.run(listOf("taken-mod", "--dir", target.toString()))
        }
        assertTrue("--force" in failed.message!!, failed.message!!)
        assertFalse(target.resolve("build.gradle.kts").exists(), "it wrote into it anyway")

        // And with --force it is the caller's business.
        New.run(listOf("taken-mod", "--dir", target.toString(), "--force"))
        assertTrue(target.resolve("build.gradle.kts").exists())
        assertEquals("mine", target.resolve("notes.txt").readText())
    }

    @Test
    fun `writes a different template when asked for one`() {
        val listens = new("noisy", "--template", "mod")
        val quiet = new("quiet", "--template", "minimal")

        assertTrue("@Subscribe" in entrypoint(listens, "Noisy"), "the default lost its listener")
        assertFalse("@Subscribe" in entrypoint(quiet, "Quiet"), "minimal grew a listener")

        assertFailsWith<Fail> { new("wrong", "--template", "nothing-like-it") }
    }

    // A mod nobody can install is a mod nobody has. Every project written here
    // is a mod repository as well, so a launcher can be pointed straight at it.
    @Test
    fun `writes a project a launcher can be pointed at`() {
        val target = new("my-mod", "--repo", "https://github.com/someone/my-mod.git")

        val registry = target.resolve("registry.toml").readText()
        // The .git comes off: this is the address a browser uses, and every
        // download link is built from it.
        assertTrue("""url = "https://github.com/someone/my-mod"""" in registry, registry)
        assertTrue(
            """releases = "https://github.com/someone/my-mod/releases/download/"""
                in registry,
            registry
        )
        // The index itself is generated after the first build, never written
        // here: there is no jar yet to describe.
        assertFalse(target.resolve(Registry.INDEX).exists())
    }

    @Test
    fun `says which line to fix when nobody said where it will live`() {
        val registry = new("homeless").resolve("registry.toml").readText()
        assertTrue("github.com/you/homeless" in registry, registry)
    }

    // The generated CI downloads a pinned build release rather than "latest",
    // and the version it pins is the one this tool was built beside -- not
    // typed in the template, for the same reason build.gradle.kts isn't.
    @Test
    fun `pins the generated workflow's coderpack download to this build's own version`() {
        val target = new("my-mod")
        val pins = target.resolve("dependencies.json").readText()
        assertTrue(
            """"path": "ancaria-dev/build", "version": "${Versions.plugin}"""" in pins,
            pins
        )
    }

    @Test
    fun `--git makes it a repository and points it at the remote`() {
        val target = new("gitty")
        val note = Git.init(target.toFile(), "https://github.com/someone/gitty")

        // Both halves are asserted, because a machine without git on the PATH
        // is a supported outcome: the project is written either way and the
        // report is the difference.
        assertEquals(note.startsWith("git init"), target.resolve(".git").exists(), note)
        if (!note.startsWith("git init")) {
            return
        }
        assertTrue("origin https://github.com/someone/gitty.git" in note, note)
        // Twice is not an error, and it does not touch what is there.
        assertTrue("already" in Git.init(target.toFile(), null))
    }

    @Test
    fun `refuses an option it does not have`() {
        assertFailsWith<Fail> { new("my-mod", "--packge", "dev.example") }
        assertFailsWith<Fail> { New.run(listOf("one", "two")) }
    }

    @Test
    fun `answers --help rather than refusing it`() {
        // Everybody types it. A tool that calls it an unknown option has taught
        // the wrong lesson on somebody's first contact with it.
        val asked = assertFailsWith<Help> { New.run(listOf("--help")) }
        assertTrue("--language" in asked.text, asked.text)
        // Anywhere in the line, and `-h` too.
        val short = assertFailsWith<Help> { New.run(listOf("my-mod", "-h")) }
        assertTrue("--template" in short.text, short.text)
        // And a wrong option is still wrong, with the same list to read.
        val wrong = assertFailsWith<Fail> { New.run(listOf("my-mod", "--langauge", "kotlin")) }
        assertTrue("--language" in wrong.message!!, wrong.message!!)
    }

    private fun entrypoint(target: Path, klass: String) =
        target.resolve("src/main/java/mods/${klass.lowercase()}/$klass.java").readText()

    private fun new(id: String, vararg options: String): Path {
        val target = directory.resolve(id)
        New.run(listOf(id, "--dir", target.toString(), *options))
        return target
    }
}
