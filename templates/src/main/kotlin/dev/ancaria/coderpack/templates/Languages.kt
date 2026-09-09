package dev.ancaria.coderpack.templates

/**
 * The languages a mod can be written in.
 *
 * A language is a directory, the way a template is: `languages/<name>/files/`
 * is copied into every project written in that language, and
 * `language.properties` says what it is. The build script lives here rather
 * than in a template because that is where it actually varies: a Kotlin mod
 * applies the Kotlin plugin and packs the standard library whichever template
 * it came from, so there is one of it per language instead of one per pair.
 * It sits one level further down, under `dsl/<dsl>/`, because it varies by the
 * syntax it is typed in as well and by nothing else: see [Dsls].
 *
 * Every key in `language.properties` except `description` becomes a
 * placeholder. That is how a shared README can name `src/main/kotlin` and how
 * the Kotlin build script can name a Kotlin version, without either one being
 * typed in Kotlin here: nothing in this file knows a language by name.
 */
object Languages {

    /** What `coderpack new` writes when nobody asked for anything else. */
    const val DEFAULT = "java"

    private const val ROOT = "languages/"

    fun names(): List<String> = Index.dirs(ROOT)

    fun require(name: String) {
        if (name !in names()) {
            throw Fail("There’s no “$name” language. Available languages: " +
                names().joinToString(", "))
        }
    }

    /**
     * What every project in this language gets, keyed by where each file goes.
     *
     * Two layers: what the language brings whichever syntax the build is typed
     * in, and the build script for this one pair. Nothing is in the first today
     * and the directory need not exist.
     */
    fun files(name: String, dsl: String): Map<String, String> {
        require(name)
        Dsls.require(dsl)
        return at("$ROOT$name/files/") + at("$ROOT$name/dsl/$dsl/")
    }

    /** The placeholders this language fills in for the files it shares with others. */
    fun values(name: String): Map<String, String> = properties(name) - "description"

    /** One sentence: what this language costs and what it gives. */
    fun describe(name: String): String = properties(name)["description"].orEmpty()

    /** One line per language, under `coderpack templates`. */
    fun list(): String = "Languages:\n" + names().joinToString("\n") {
        "  %-9s %s".format(it, describe(it))
    }

    private fun at(prefix: String): Map<String, String> =
        Index.under(prefix).associate { it.removePrefix(prefix) to Index.text(it) }

    private fun properties(name: String) = Index.properties("$ROOT$name/language.properties")
}
