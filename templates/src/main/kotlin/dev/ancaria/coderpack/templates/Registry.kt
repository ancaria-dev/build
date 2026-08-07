package dev.ancaria.coderpack.templates

import dev.ancaria.coderpack.verify.Verifier
import java.io.File
import java.security.MessageDigest

/**
 * `coderpack index`: the file a launcher fetches to find out what a repository
 * of mods has in it.
 *
 * The index is generated, never written by hand, and that is the whole point of
 * it. Every field describing a mod is copied out of the descriptor inside the
 * jar that was built, so the version a player is offered is the version in the
 * jar they get. A hand-kept list says whatever somebody last remembered to type.
 *
 * What a human owns is [SOURCE], which says who the repository is and where its
 * releases live. Six lines, and none of them change when a mod does.
 *
 * A jar the linter has anything to say about is not indexed at all. A registry
 * is the last place a broken jar should be reachable from, and refusing here
 * costs a release that fails rather than a download that does.
 */
object Registry {

    /** The contract: this file at the root of a repository makes it one. */
    const val INDEX = "sacred.mods.repository.json"

    /** What a person writes. Read with the same parser as a mod descriptor. */
    const val SOURCE = "registry.toml"

    /** The layout version, so a launcher can refuse a repository from the future. */
    const val SRML = 1

    val HELP = """
        Usage: coderpack index [<repository>] [options]

          --jars <dir>   where the built jars are. By default every
                         <repository>/*/build/sacred-mod/*.jar, and the
                         repository's own when it holds a single mod
          --out <file>   where to write. By default <repository>/$INDEX
          --check        write nothing; exit 1 when the file on disk is not what
                         this run would have written
    """.trimIndent()

    fun run(argv: List<String>): Int {
        val args = Args.parse(argv, setOf("jars", "out"), setOf("check"), HELP)
        val root = File(args.rest.firstOrNull() ?: ".")
        if (!root.isDirectory) {
            throw Fail("${root.path}: directory not found.")
        }

        val header = header(root)
        val jars = jars(root, args.option("jars"))
        if (jars.isEmpty()) {
            throw Fail("No mod jars found. Build them first or select their directory with --jars.")
        }

        val alone = jars.size == 1
        val entries = jars.sortedBy { it.name }.map { entry(root, it, header, alone) }
        val duplicate = entries.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicate.isNotEmpty()) {
            // Two jars claiming one id is two answers to "install this mod".
            throw Fail("Multiple jars use the same mod id: " + duplicate.joinToString(", "))
        }
        val text = write(header, entries.sortedBy { it.id })

        val out = File(args.option("out") ?: File(root, INDEX).path)
        if (args.flag("check")) {
            val current = if (out.isFile) out.readText() else ""
            if (current == text) {
                println("$INDEX is up to date (${entries.size} ${mods(entries.size)}).")
                return 0
            }
            System.err.println("coderpack: $INDEX doesn’t match the built jars. " +
                "Run `coderpack index` and commit the result.")
            return 1
        }
        out.writeText(text)
        for (entry in entries.sortedBy { it.id }) {
            println("  ${entry.id} ${entry.version}  ${entry.file}")
        }
        println("Wrote ${entries.size} ${mods(entries.size)} to ${out.path}.")
        return 0
    }

    /** One mod, as far as this file is concerned. */
    private class Entry(val id: String, val version: String, val file: String, val json: String)

    private fun header(root: File): Map<String, String> {
        val file = File(root, SOURCE)
        if (!file.isFile) {
            throw Fail("$SOURCE wasn’t found in ${root.path}. Create it with these six lines:\n\n" +
                EXAMPLE)
        }
        val values = Verifier.parse(file.readText())
        for (required in listOf("name", "url", "releases")) {
            if (values[required].isNullOrBlank()) {
                throw Fail("$SOURCE is missing $required.\n\n" + EXAMPLE)
            }
        }
        if (!values.getValue("releases").contains("{file}")) {
            throw Fail("$SOURCE: releases must contain {file}; otherwise every mod in " +
                "this repository gets the same download URL.")
        }
        return values
    }

    private fun jars(root: File, given: String?): List<File> {
        if (given != null) {
            val dir = File(given)
            if (!dir.isDirectory) {
                throw Fail("$given: directory not found.")
            }
            return dir.listFiles { file: File -> file.extension == "jar" }?.toList().orEmpty()
        }
        // A repository of several mods keeps each in a directory of its own; one
        // written by `coderpack new` is a single mod at the root, and its jar
        // lands one level higher. Both layouts come out of this same tool, so
        // both are looked for rather than one being the real one.
        return packed(root) + root.listFiles { file: File -> file.isDirectory }.orEmpty()
            .flatMap { packed(it) }
    }

    private fun packed(project: File): List<File> =
        File(project, "build/sacred-mod")
            .listFiles { file: File -> file.extension == "jar" }?.toList().orEmpty()

    private fun entry(root: File, jar: File, header: Map<String, String>, alone: Boolean): Entry {
        val report = Verifier.verify(jar.toPath())
        if (!report.ok()) {
            throw Fail("${jar.name} failed verification and won’t be added to the registry:\n\n$report")
        }
        val values = Verifier.describe(jar.toPath())
        val id = values["id"].orEmpty()
        val version = values["version"] ?: "0"
        val bytes = jar.readBytes()

        // Both are a convention rather than a field: a mod's directory is named
        // after its id, so there is nothing to keep in step and nothing to get
        // wrong. A repository laid out some other way simply has neither.
        val source = File(root, id).takeIf { it.isDirectory }?.let { id }
        val icon = File(root, "$id/icon.png").takeIf { it.isFile }?.let { "$id/icon.png" }
        // A repository holding one mod is that mod's page, so the picture at its
        // root is that mod's picture. With several in it, that picture belongs
        // to the repository and a mod without its own simply has none.
            ?: File(root, "icon.png").takeIf { alone && it.isFile }?.let { "icon.png" }

        val url = header.getValue("releases")
            .replace("{id}", id)
            .replace("{version}", version)
            .replace("{file}", jar.name)

        val json = Json.obj("    ", listOf(
            "id" to Json.string(id),
            "name" to Json.string(values["name"] ?: id),
            "version" to Json.string(version),
            "description" to values["description"]?.let { Json.string(it) },
            "api" to Json.string(values["api"].orEmpty()),
            // Only when the mod named one. An absent range means "any release",
            // and writing that out as an empty string would be a launcher
            // reading a constraint where the author put none.
            "loader" to values["loader"]?.takeIf { it.isNotBlank() }?.let { Json.string(it) },
            "authors" to Verifier.list(values["authors"])
                .takeIf { it.isNotEmpty() }?.let { Json.strings(it) },
            "website" to values["website"]?.let { Json.string(it) },
            "conflicts" to Verifier.list(values["conflicts"])
                .takeIf { it.isNotEmpty() }?.let { Json.strings(it) },
            "source" to source?.let { Json.string(it) },
            "icon" to icon?.let { Json.string(it) },
            "file" to Json.string(jar.name),
            "size" to bytes.size.toString(),
            "sha256" to Json.string(sha256(bytes)),
            "url" to Json.string(url),
        ))
        return Entry(id, version, jar.name, json)
    }

    /**
     * No timestamp anywhere in here, deliberately. A generated file with the
     * time in it is a file that differs from itself, which would make `--check`
     * meaningless and every commit a diff.
     */
    private fun write(header: Map<String, String>, entries: List<Entry>): String {
        val mods = entries.joinToString(",\n", "[\n", "\n  ]") { "    " + it.json }
        return Json.obj("", listOf(
            "srml" to SRML.toString(),
            "name" to Json.string(header.getValue("name")),
            "description" to header["description"]?.let { Json.string(it) },
            "url" to Json.string(header.getValue("url")),
            "icon" to header["icon"]?.let { Json.string(it) },
            "mods" to mods,
        )) + "\n"
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun mods(amount: Int) = if (amount == 1) "mod" else "mods"

    private val EXAMPLE = """
        name = "Ancaria"
        description = "Mods from the people who wrote the loader"
        url = "https://github.com/ancaria-dev/mods"
        icon = "icon.png"
        releases = "https://github.com/ancaria-dev/mods/releases/download/{id}-v{version}/{file}"
    """.trimIndent()
}
