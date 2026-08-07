package dev.ancaria.coderpack.templates

import java.util.Properties

/**
 * The directories of files baked into this jar, listed at build time.
 *
 * A jar cannot be asked what is inside one of its packages, so a tool that
 * ships trees of files either knows them by name in code or reads a list. The
 * build walks `src/main/resources` and writes that list, and [Templates] and
 * [Languages] read nothing else. That is the whole reason a new template, and a
 * new language, is a matter of adding files.
 */
object Index {

    /** Written by the build, next to the classes, so no jar beside us shadows it. */
    private const val INDEX = "/dev/ancaria/coderpack/templates/index.txt"

    /** Every resource the build put in this jar, one path per line. */
    val paths: List<String> by lazy {
        resource(INDEX).lines().filter { it.isNotBlank() }
    }

    fun under(prefix: String): List<String> = paths.filter { it.startsWith(prefix) }

    /** The directory names directly below [prefix], sorted and without repeats. */
    fun dirs(prefix: String): List<String> =
        under(prefix).map { it.removePrefix(prefix).substringBefore('/') }.distinct().sorted()

    fun text(path: String): String = resource("/$path")

    /** A `.properties` resource, or nothing at all when there is no such file. */
    fun properties(path: String): Map<String, String> {
        if (path !in paths) return emptyMap()
        val loaded = Properties().apply { load(text(path).reader()) }
        return loaded.stringPropertyNames().associateWith { loaded.getProperty(it) }
    }

    private fun resource(path: String): String =
        Index::class.java.getResourceAsStream(path)?.use { it.readBytes().decodeToString() }
            ?: error("$path is missing from this jar")
}
