package dev.ancaria.coderpack.templates

import java.util.Properties

/**
 * The version numbers a generated project is written with.
 *
 * Filled in by the build rather than typed here, so the plugin a scaffolded mod
 * applies is the plugin this tool was built beside. A scaffolder that names a
 * version nobody published produces a project that does not resolve.
 */
object Versions {

    private val values = Properties().apply {
        Versions::class.java.getResourceAsStream("coderpack.properties")!!.use { load(it) }
    }

    /** This tool, and the Gradle plugin: one build, one number. */
    val tool: String = values.getProperty("tool")

    val plugin: String = values.getProperty("plugin")

    /** The artifact version of `dev.ancaria.coderpack:api`, which moves on its own. */
    val api: String = values.getProperty("api")
}
