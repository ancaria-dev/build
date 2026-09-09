package dev.ancaria.coderpack.templates

/**
 * The Gradle wrapper every scaffolded project gets.
 *
 * One command has to be enough, and a project that then asks for a system
 * Gradle of the right version is not one command. So the wrapper travels
 * inside this tool, but not as a second copy checked in beside the templates:
 * the build stages this repository's own `gradlew`, `gradlew.bat` and
 * `gradle-wrapper.jar` as resources, so the distribution a generated mod
 * downloads is the one this build is tested with, and there is nothing to keep
 * in step by hand.
 */
object Wrapper {

    /** The one that has to come out executable on anything but Windows. */
    const val SCRIPT = "gradlew"

    private const val ROOT = "/wrapper/"

    private val FILES = listOf(
        SCRIPT,
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties",
    )

    fun files(): Map<String, ByteArray> = FILES.associateWith { bytes(ROOT + it) }

    private fun bytes(resource: String): ByteArray =
        Wrapper::class.java.getResourceAsStream(resource)?.use { it.readBytes() }
            ?: error("$resource is missing from this jar")
}
