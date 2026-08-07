package dev.ancaria.coderpack.templates

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

/**
 * A template plus a handful of values, written out as a project.
 *
 * Two steps, and the split is the point: [plan] renders every file and every
 * file name into memory, and [write] puts them on disk. Anything wrong with the
 * template, the values or the target directory has thrown by then, so a run
 * that fails leaves nothing behind for somebody to clear out before trying
 * again.
 */
object Scaffold {

    // Not preceded by a dollar. A workflow file is full of `${{ github.token }}`,
    // which is GitHub's syntax and not a placeholder nothing fills in, and a
    // template that ships CI carries both shapes in the same file.
    private val PLACEHOLDER = Regex("(?<!\\\$)\\{\\{(\\w+)}}")

    /** Every file the project will have, keyed by its path inside the project. */
    fun plan(
        template: String,
        language: String,
        values: Map<String, String>,
        dsl: String = Dsls.DEFAULT,
        repository: Boolean = true,
    ): Map<String, ByteArray> {
        // The DSL and the language fill in what a file shared between them
        // cannot know -- which build file, which source directory, which file
        // extension, which compiler version -- and both lose every argument
        // with what the caller asked for.
        val all = Dsls.values(dsl) + Languages.values(language) + values
        val files = sortedMapOf<String, ByteArray>()
        for ((path, text) in Templates.files(template, language, dsl, repository)) {
            files[dotted(render(path, all))] = render(text, all).toByteArray()
        }
        files.putAll(Wrapper.files())
        return files
    }

    /**
     * `{{id}}` and the rest, in a file and in its name alike: the entrypoint
     * lives at a path only the package knows. A placeholder nothing fills in is
     * a mistake in the template, and it stops here rather than reaching a
     * project as literal text.
     */
    fun render(text: String, values: Map<String, String>): String =
        PLACEHOLDER.replace(text) { match ->
            val name = match.groupValues[1]
            values[name] ?: throw Fail("The template requires {{$name}}, but no value was provided.")
        }

    /**
     * `_gitignore` in a template is `.gitignore` in the project.
     *
     * Not a preference. A jar is built by a copy, and a copy skips `.gitignore`
     * and every other dotfile Ant decided long ago nobody means to package, so a
     * template that spelled it with the dot would ship without it.
     */
    private fun dotted(path: String): String =
        path.split('/').joinToString("/") { if (it.startsWith("_")) "." + it.drop(1) else it }

    fun write(target: Path, files: Map<String, ByteArray>, force: Boolean) {
        if (Files.exists(target) && !Files.isDirectory(target)) {
            throw Fail("$target is a file, not a directory.")
        }
        if (!force && Files.isDirectory(target) && used(target)) {
            throw Fail("$target isn’t empty. Pass --force to write into it anyway.")
        }
        for ((path, bytes) in files) {
            val file = target.resolve(path)
            Files.createDirectories(file.parent)
            Files.write(file, bytes)
        }
        executable(target.resolve(Wrapper.SCRIPT))
    }

    private fun used(directory: Path): Boolean =
        Files.list(directory).use { it.findAny().isPresent }

    /** Nothing to do on Windows, where there is no such bit and the .bat runs. */
    private fun executable(script: Path) {
        val view = Files.getFileAttributeView(script, PosixFileAttributeView::class.java) ?: return
        view.setPermissions(
            view.readAttributes().permissions() + setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE,
            )
        )
    }
}
