package dev.ancaria.coderpack.templates

import java.io.File
import java.io.IOException

/**
 * `git init` in a project that has just been written, and the remote if one was
 * named.
 *
 * Nothing here fails a run. The project is already on disk by the time any of
 * it happens, and a machine without git on the PATH has a perfectly good
 * project that is simply not a repository yet -- so this reports a line and
 * gets out of the way rather than throwing after the fact.
 *
 * No commit is made. What goes in the first commit, and under whose name, is
 * not a scaffolder's decision, and `git status` in a fresh project says exactly
 * what is there.
 */
object Git {

    /** One line for the report: what happened, or why nothing did. */
    fun init(target: File, remote: String?): String {
        if (File(target, ".git").exists()) {
            return "$target is a git repository already; left alone"
        }
        if (!run(target, "git", "init", "--quiet")) {
            return "could not run git, so $target is not a repository yet. " +
                "Install git and run `git init` in it"
        }
        if (remote == null) {
            return "git init"
        }
        val url = if (remote.endsWith(".git")) remote else "$remote.git"
        return if (run(target, "git", "remote", "add", "origin", url)) {
            "git init, origin $url"
        } else {
            "git init. The remote could not be added; do it with " +
                "`git remote add origin $url`"
        }
    }

    private fun run(target: File, vararg command: String): Boolean = try {
        ProcessBuilder(*command)
            .directory(target)
            // Nothing git prints belongs in this tool's output: the report says
            // what happened in one line, and a failure says so too.
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor() == 0
    } catch (noGit: IOException) {
        false
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        false
    }
}
