package dev.ancaria.coderpack.templates

/**
 * Which syntax the project's own build is written in.
 *
 * A directory, the way a template and a language are: `dsl/<name>/files/` is
 * copied into every project written in that syntax, which today is the settings
 * file, and `dsl.properties` says what it is. The build script itself is not
 * here, because it varies by language as well -- a Kotlin mod applies the
 * Kotlin plugin whichever syntax the script is typed in -- so it sits under
 * `languages/<language>/dsl/<dsl>/` and there is one of it per pair.
 *
 * This is not the same question as [Languages]. That one is what the mod is
 * written in and reaches the player as bytecode; this one is what the build
 * file is written in and reaches nobody. A Java mod with a `build.gradle` and a
 * Kotlin mod with a `build.gradle.kts` are both ordinary.
 *
 * Every key in `dsl.properties` except `description` becomes a placeholder,
 * which is how one shared README names `build.gradle.kts` in one project and
 * `build.gradle` in the next without either name being typed in Kotlin here.
 */
object Dsls {

    /** What comes out when nobody says. Kotlin, because that is the project's own build. */
    const val DEFAULT = "kotlin"

    private const val ROOT = "dsl/"

    fun names(): List<String> = Index.dirs(ROOT)

    fun require(name: String) {
        if (name !in names()) {
            throw Fail("There’s no “$name” build DSL. Available build DSLs: " +
                names().joinToString(", "))
        }
    }

    /** What every project in this syntax gets, keyed by where each file goes. */
    fun files(name: String): Map<String, String> {
        require(name)
        val prefix = "$ROOT$name/files/"
        return Index.under(prefix).associate { it.removePrefix(prefix) to Index.text(it) }
    }

    /** The placeholders this syntax fills in: `{{buildFile}}` and `{{settingsFile}}`. */
    fun values(name: String): Map<String, String> = properties(name) - "description"

    /** One sentence: which file it writes and what reading it is like. */
    fun describe(name: String): String = properties(name)["description"].orEmpty()

    /** One line per syntax, under `coderpack templates`. */
    fun list(): String = "Build scripts:\n" + names().joinToString("\n") {
        "  %-9s %s".format(it, describe(it))
    }

    private fun properties(name: String) = Index.properties("$ROOT$name/dsl.properties")
}
