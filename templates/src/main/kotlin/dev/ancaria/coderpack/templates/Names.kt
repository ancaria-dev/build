package dev.ancaria.coderpack.templates

import dev.ancaria.coderpack.verify.Ids

/**
 * Everything a project is called, worked out from the one word the caller
 * typed.
 *
 * `coderpack new my-mod` has to reach a mod id, a display name, a package and a
 * class name, and three of those four are guesses. They are guessed in one
 * place, and `--package`, `--display-name` and `--author` override the ones
 * somebody actually minds about.
 *
 * The id itself is never guessed. It is checked against [Ids], the same rule
 * the Gradle plugin enforces and the linter reports on, because a scaffolder
 * that quietly rewrote the name into something legal would hand back a project
 * that is not the one that was asked for.
 */
object Names {

    private val SEGMENT = Regex("[A-Za-z_][A-Za-z0-9_]*")

    fun id(text: String): String {
        val id = text.trim()
        if (!Ids.valid(id)) {
            throw Fail(
                "“$id” isn’t a valid mod id. It must match ${Ids.PATTERN}: lowercase " +
                    "letters, digits and inner hyphens, because the launcher writes it " +
                    "into its enabled list. Did you mean “${suggest(id)}”?"
            )
        }
        return id
    }

    /** The closest legal id to what was typed, which is all the message above needs. */
    fun suggest(text: String): String {
        val letters = text.lowercase()
            .map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }
            .joinToString("")
        val cleaned = letters.split('-').filter { it.isNotEmpty() }.joinToString("-")
        return cleaned.ifEmpty { "my-mod" }
    }

    /** `my-mod` is `My Mod` to a player. */
    fun display(id: String): String =
        id.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

    /**
     * `mods.mymod`. Not the author's own namespace, which this tool has no way
     * to know, and not `com.example`, which nobody remembers to change.
     */
    fun pkg(id: String): String = "mods." + compiles(id.replace("-", ""), "m")

    /** `my-mod` gives `MyMod`, which is the class the descriptor will name. */
    fun klass(id: String): String = compiles(
        id.split('-').joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }, "Mod"
    )

    /** A package somebody typed: refused rather than repaired. */
    fun validPkg(text: String): String {
        val pkg = text.trim()
        if (pkg.isEmpty() || pkg.split('.').any { !SEGMENT.matches(it) }) {
            throw Fail(
                "“$pkg” isn’t a valid Java package. Use dot-separated words that each " +
                    "start with a letter or underscore."
            )
        }
        return pkg
    }

    /** An id may start with a digit and a Java name may not. */
    private fun compiles(word: String, prefix: String): String = when {
        word.isEmpty() -> prefix
        word.first().isDigit() -> prefix + word
        else -> word
    }
}
