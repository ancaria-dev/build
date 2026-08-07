package dev.ancaria.coderpack.templates

/**
 * The project templates baked into this jar.
 *
 * A template is a directory of files and nothing else. `<name>/files/` is
 * copied into the new project whichever language it is written in;
 * `<name>/lang/<language>/` is copied only for that one, and holds the one
 * thing no two languages can share, which is the source of the entrypoint. What
 * varies by language and not by template -- the build script -- comes from
 * [Languages] instead, so neither part is written out twice.
 *
 * What varies by neither is [COMMON], written into every project whatever it
 * was asked for. Today that is the `.gitignore` and nothing else: the settings
 * file went to [Dsls], since its name and its syntax are the one thing a build
 * DSL decides on its own, and [REPOSITORY] is asked for rather than given.
 *
 * [REPOSITORY] is the SRML half -- the `registry.toml` a launcher is pointed at
 * and the workflow that cuts the releases it links to. It is a layer rather
 * than a template because it is orthogonal to all three of the others: any
 * template, in any language, in either syntax, is either a mod somebody
 * installs by hand or a mod repository somebody subscribes to.
 *
 * Adding a template is adding those files. The build walks the directory and
 * writes an index, because a jar has no way to list what is in a package, and
 * nothing in this file knows any template or any language by name.
 */
object Templates {

    /** What `coderpack new` writes when nobody asked for anything else. */
    const val DEFAULT = "mod"

    private const val ROOT = "templates/"

    /** Files every project gets, whichever template and whichever language. */
    private const val COMMON = "common/files/"

    /** What makes a project a mod repository as well as a mod. SRML, in two files. */
    private const val REPOSITORY = "repository/files/"

    fun names(): List<String> = Index.dirs(ROOT)

    fun require(name: String) {
        if (name !in names()) {
            throw Fail("There’s no “$name” template. Available templates: " +
                names().joinToString(", "))
        }
    }

    /** The languages this template has an entrypoint written in. */
    fun languages(name: String): List<String> = Index.dirs("$ROOT$name/lang/")

    /**
     * The files of one template in one language, keyed by where each goes in
     * the new project.
     *
     * Six layers, most specific last: what every project gets, what SRML adds
     * when it was asked for, what the build DSL brings, what the language
     * brings, what the template shares between languages, and what the template
     * writes for this language alone. A template that had to say something
     * different about its build script or its settings file can therefore say
     * it, and none of them does today.
     */
    fun files(
        name: String,
        language: String,
        dsl: String = Dsls.DEFAULT,
        repository: Boolean = true,
    ): Map<String, String> {
        require(name)
        Languages.require(language)
        Dsls.require(dsl)
        if (language !in languages(name)) {
            throw Fail(
                "The “$name” template isn’t available in $language. Available languages: " +
                    languages(name).joinToString(", ")
            )
        }
        return at(COMMON) +
            (if (repository) at(REPOSITORY) else emptyMap()) +
            Dsls.files(dsl) +
            Languages.files(language, dsl) +
            at("$ROOT$name/files/") +
            at("$ROOT$name/lang/$language/")
    }

    /** Two lines per template, for `coderpack templates`: what it is, and in what. */
    fun list(): String = "Templates:\n" + names().joinToString("\n") {
        "  %-9s %s\n%12s%s".format(it, describe(it), "", languages(it).joinToString(", "))
    }

    private fun at(prefix: String): Map<String, String> =
        Index.under(prefix).associate { it.removePrefix(prefix) to Index.text(it) }

    /** One sentence: what this template is for. Read by the IDE plugin's combo box too. */
    fun describe(name: String): String =
        Index.properties("$ROOT$name/template.properties")["description"].orEmpty()
}
