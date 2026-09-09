package dev.ancaria.coderpack.templates

import java.nio.file.Path

/**
 * `coderpack new <name>`: one command, and what comes out builds.
 *
 * The name is the mod id, and everything else has a default derived from it. A
 * project written here applies the published plugin, compiles against the
 * loader API, carries a filled-in `sacred { }` block and an entrypoint the
 * linter accepts, and brings its own Gradle wrapper. There is nothing to
 * install first and nothing to copy out of somebody else's build file.
 *
 * `--language` picks what the mod itself is written in, and Java is what comes
 * out when nobody says. `--dsl` picks what the project's own build is typed in,
 * which is a different question with a different answer: Kotlin DSL unless
 * somebody asks for `build.gradle` instead.
 *
 * `--no-registry` writes a mod and nothing else. Without it the project is also
 * an SRML repository, a `registry.toml` and a workflow that cuts releases,
 * so a launcher can be pointed straight at it and a mod nobody can install is
 * not the default outcome.
 */
object New {

    private val OPTIONS = setOf(
        "template", "language", "dsl", "dir", "package", "display-name", "description",
        "author", "mod-version", "repo",
    )

    private val FLAGS = setOf("force", "git", "no-registry")

    /**
     * What `--repo` says when nobody says anything.
     *
     * A scaffolder cannot know where a project will live, and the one line in
     * `registry.toml` that has to be right is the one every download link is
     * built from. So it is written wrong on purpose, in a shape nobody mistakes
     * for a working address, and the report says which line to fix.
     */
    private const val NOWHERE = "https://github.com/you"

    fun repository(id: String, given: String?): String {
        val url = (given ?: "$NOWHERE/$id").trim().trimEnd('/')
        return url.removeSuffix(".git")
    }

    /** The options, one per line. `coderpack help` prints the same lines. */
    val HELP = """
        --template <name>      which template to write     (default: ${Templates.DEFAULT})
        --language <name>      what the mod is written in  (default: ${Languages.DEFAULT})
        --dsl <name>           what the build is typed in  (default: ${Dsls.DEFAULT})
        --dir <path>           where to write it           (default: ./<name>)
        --package <package>    the Java package            (default: mods.<name>)
        --display-name <text>  the name a player reads     (default: from <name>)
        --description <text>   one sentence for the mod list
        --author <name>        who wrote it                (default: this account)
        --mod-version <ver>    the mod's own version       (default: 1.0.0)
        --repo <url>           where the project will live, which is what every
                               download link in registry.toml is built from
        --no-registry          just a mod, not a mod repository: no registry.toml
                               and no release workflow
        --git                  run git init in it, and add --repo as origin
        --force                write into a directory that is not empty
    """.trimIndent().prependIndent("  ")

    fun usage(): String = "Usage: coderpack new <name> [options]\n\nOptions:\n" + HELP

    fun run(argv: List<String>): Int {
        val args = Args.parse(argv, OPTIONS, FLAGS, usage())
        if (args.rest.size != 1) {
            throw Fail(usage())
        }

        val id = Names.id(args.rest.first())
        val template = args.option("template") ?: Templates.DEFAULT
        val language = args.option("language") ?: Languages.DEFAULT
        val dsl = args.option("dsl") ?: Dsls.DEFAULT
        // SRML: whether the project describes itself to a launcher as well as
        // being a mod. Named for the layout rather than for the file, because
        // it is two files and a workflow rather than one.
        val srml = !args.flag("no-registry")
        val pkg = args.option("package")?.let(Names::validPkg) ?: Names.pkg(id)
        val klass = Names.klass(id)
        val name = args.option("display-name") ?: Names.display(id)
        val repo = repository(id, args.option("repo"))

        val values = mapOf(
            "id" to id,
            "name" to name,
            "description" to (args.option("description") ?: "$name, a mod for Sacred Gold"),
            "version" to (args.option("mod-version") ?: "1.0.0"),
            "package" to pkg,
            "packagePath" to pkg.replace('.', '/'),
            "class" to klass,
            "entrypoint" to "$pkg.$klass",
            "author" to (args.option("author") ?: System.getProperty("user.name") ?: "unknown"),
            "repo" to repo,
            "plugin" to Versions.plugin,
            "api" to Versions.api,
        )

        // Everything is rendered before anything is written, and the directory
        // is checked before the first file, so a run that fails leaves no half
        // of a project behind.
        val files = Scaffold.plan(template, language, values, dsl, srml)
        val target = Path.of(args.option("dir") ?: id)
        Scaffold.write(target, files, args.flag("force"))

        // After the files, and never able to undo them: see Git.
        val git = if (args.flag("git")) Git.init(target.toFile(), repo) else null
        report(target, template, language, dsl, files.keys, git, srml, args.option("repo") != null)
        return 0
    }

    private fun report(
        target: Path,
        template: String,
        language: String,
        dsl: String,
        paths: Set<String>,
        git: String?,
        srml: Boolean,
        told: Boolean,
    ) {
        println("Wrote $target from the “$template” template in $language, using the $dsl build DSL.")
        paths.forEach { println("  $it") }
        git?.let { println("  $it") }
        println()
        println("Next:")
        println("  cd $target")
        if (srml && !told) {
            // The one line in a generated project that is wrong until somebody
            // says otherwise, said once, where it will be read.
            println("  Set url in registry.toml to the project’s future location.")
        }
        println("  ./gradlew assembleSacredMod")
        println("  ./gradlew installSacredMod -PsacredDir=\"<Sacred Gold>\"")
        if (srml) {
            println()
            println("To make the mod installable from a launcher:")
            println("  coderpack index")
            println("  Push the project. .github/workflows/build.yml will release the jar.")
        }
    }
}
