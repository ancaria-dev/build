package dev.ancaria.coderpack.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Placeholders, and the templates, languages and build DSLs the build found
 * without being told about any of them.
 */
class ScaffoldTest {

    private val values = mapOf(
        "id" to "my-mod",
        "name" to "My Mod",
        "description" to "One sentence",
        "version" to "1.0.0",
        "package" to "dev.example.mymod",
        "packagePath" to "dev/example/mymod",
        "class" to "MyMod",
        "entrypoint" to "dev.example.mymod.MyMod",
        "author" to "Somebody",
        "plugin" to "0.1.0",
        "api" to "0.1.0",
        "repo" to "https://github.com/somebody/my-mod",
    )

    /**
     * A placeholder that nothing filled in.
     *
     * `${{ github.token }}` in the workflow file is not one: that is GitHub's
     * own syntax, it is meant to reach the project exactly as written, and the
     * renderer leaves it alone for the same reason this does.
     */
    private val LEFTOVER = Regex("(?<!\\\$)\\{\\{")

    @Test
    fun `fills a placeholder in a file and in its name`() {
        assertEquals("id = \"my-mod\"", Scaffold.render("id = \"{{id}}\"", values))
        assertEquals(
            "src/main/java/dev/example/mymod/MyMod.java",
            Scaffold.render("src/main/java/{{packagePath}}/{{class}}.java", values)
        )
    }

    @Test
    fun `stops on a placeholder nothing fills in`() {
        val failed = assertFailsWith<Fail> { Scaffold.render("{{nobody}}", values) }
        assertTrue("{{nobody}}" in failed.message!!, failed.message!!)
    }

    @Test
    fun `leaves no placeholder anywhere in a planned project`() {
        // Every combination, because each axis fills in placeholders the other
        // two use. One that only one side knows about would reach a project as
        // literal text.
        for ((template, language, dsl) in triples()) {
            for ((path, bytes) in Scaffold.plan(template, language, values, dsl)) {
                if (!path.endsWith(".jar")) {
                    val left = LEFTOVER.find(bytes.decodeToString())
                    assertTrue(left == null, "$template/$language/$dsl: $path has ${left?.value}")
                }
                assertTrue(LEFTOVER.find(path) == null, "$template/$language/$dsl: $path")
            }
        }
    }

    @Test
    fun `plans the files every project gets, whichever template`() {
        for ((template, language, dsl) in triples()) {
            val planned = Scaffold.plan(template, language, values, dsl)
            val where = Dsls.values(dsl)
            val everyProject = listOf(
                "registry.toml",
                where.getValue("settingsFile"),
                where.getValue("buildFile"),
                ".gitignore",
                // A leading `_` becomes a dot, or the copy that builds this jar
                // would have dropped the whole directory on the floor.
                ".github/workflows/build.yml",
            )
            for (shared in everyProject) {
                assertTrue(shared in planned.keys, "$template/$language/$dsl has no $shared")
            }
            // The one line in there that decides where every download comes
            // from, and the only reason `registry.toml` needs a value at all.
            val registry = planned.getValue("registry.toml").decodeToString()
            assertTrue(
                """url = "https://github.com/somebody/my-mod"""" in registry, registry
            )
            assertTrue("{id}-v{version}" in registry, registry)
        }
    }

    /**
     * A project that is a mod and nothing else.
     *
     * SRML is a layer rather than a template, so what this asserts is that
     * dropping it drops exactly three files and touches nothing else: a
     * project without a registry still has its build script, its entrypoint
     * and its wrapper.
     */
    @Test
    fun `plans no registry and no workflow when SRML was not asked for`() {
        for ((template, language, dsl) in triples()) {
            val bare = Scaffold.plan(template, language, values, dsl, repository = false)
            val full = Scaffold.plan(template, language, values, dsl)
            assertEquals(
                setOf("registry.toml", ".github/workflows/build.yml", "dependencies.json"),
                full.keys - bare.keys,
                "$template/$language/$dsl"
            )
        }
    }

    /**
     * The build script, in the syntax that was asked for and no other.
     *
     * Both files do the same job under different names, so a project that ended
     * up with the pair would build whichever one Gradle prefers and silently
     * ignore what somebody typed in the other.
     */
    @Test
    fun `plans one build script and one settings file, in the DSL that was asked for`() {
        val either = setOf(
            "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts"
        )
        for ((template, language, dsl) in triples()) {
            val where = Dsls.values(dsl)
            val scripts = Scaffold.plan(template, language, values, dsl).keys.filter { it in either }
            assertEquals(
                setOf(where.getValue("buildFile"), where.getValue("settingsFile")),
                scripts.toSet(),
                "$template/$language/$dsl"
            )
        }
    }

    @Test
    fun `finds the templates the build indexed`() {
        assertTrue(Templates.DEFAULT in Templates.names())
        assertTrue(Templates.names().size >= 2, Templates.names().toString())
        assertFailsWith<Fail> { Templates.files("nothing-like-it", Languages.DEFAULT) }
    }

    @Test
    fun `finds the languages the build indexed`() {
        assertTrue(
            Languages.names().containsAll(listOf("groovy", "java", "kotlin")),
            Languages.names().toString()
        )
        assertTrue(Languages.DEFAULT in Languages.names())
        for (template in Templates.names()) {
            // A template offers what it has an entrypoint for, and each of those
            // has to be a language this build actually indexed.
            assertTrue(Languages.DEFAULT in Templates.languages(template), template)
            assertTrue(Languages.names().containsAll(Templates.languages(template)), template)
        }
    }

    @Test
    fun `finds the build DSLs the build indexed`() {
        assertTrue(Dsls.names().containsAll(listOf("groovy", "kotlin")), Dsls.names().toString())
        assertTrue(Dsls.DEFAULT in Dsls.names())
        // Each one names its own two files, because a shared README asks it what
        // the build script is called and so does the test above.
        for (dsl in Dsls.names()) {
            val where = Dsls.values(dsl)
            assertTrue("buildFile" in where, "$dsl: $where")
            assertTrue("settingsFile" in where, "$dsl: $where")
        }
        val unknown = assertFailsWith<Fail> {
            Scaffold.plan(Templates.DEFAULT, Languages.DEFAULT, values, "make")
        }
        assertTrue("make" in unknown.message!!, unknown.message!!)
        assertTrue("kotlin" in unknown.message!!, unknown.message!!)
    }

    @Test
    fun `refuses a language nothing was written in`() {
        val unknown = assertFailsWith<Fail> { Templates.files(Templates.DEFAULT, "rust") }
        assertTrue("rust" in unknown.message!!, unknown.message!!)
        // And says which ones there are, because a refusal that does not is a
        // refusal somebody guesses at.
        assertTrue("kotlin" in unknown.message!!, unknown.message!!)
        assertFailsWith<Fail> { Scaffold.plan(Templates.DEFAULT, "rust", values) }
    }

    @Test
    fun `plans one entrypoint, where the language says it goes`() {
        for ((template, language, dsl) in triples()) {
            val where = Languages.values(language)
            val sources = Scaffold.plan(template, language, values, dsl).keys
                .filter { it.startsWith("src/") }
            assertEquals(
                listOf("src/main/${where["srcDir"]}/dev/example/mymod/MyMod.${where["srcExt"]}"),
                sources,
                "$template/$language/$dsl"
            )
        }
    }

    @Test
    fun `says what every template, every language and every build script is for`() {
        // This is `coderpack templates`, which is where a mod author finds out
        // that --language and --dsl exist at all.
        val listed = Templates.list() + "\n" + Languages.list() + "\n" + Dsls.list()
        for ((template, language, dsl) in triples()) {
            assertTrue(template in listed, listed)
            assertTrue(language in listed, listed)
            assertTrue(dsl in listed, listed)
        }
        // The description of a template, of a language, and of a build DSL.
        assertTrue("An entrypoint and one working listener" in listed, listed)
        assertTrue("standard library" in listed, listed)
        assertTrue("build.gradle.kts" in listed, listed)
    }

    private fun pairs(): List<Pair<String, String>> =
        Templates.names().flatMap { template ->
            Templates.languages(template).map { template to it }
        }

    /**
     * Every template, in every language it is written in, in every build DSL.
     *
     * Twelve of them today, out of twenty-five files. That is the whole point of
     * splitting the trees the way they are split: the entrypoint varies by
     * template and language, the build script by language and DSL, the settings
     * file by DSL alone, and nothing is written out twice.
     */
    private fun triples(): List<Triple<String, String, String>> =
        pairs().flatMap { (template, language) ->
            Dsls.names().map { Triple(template, language, it) }
        }
}
