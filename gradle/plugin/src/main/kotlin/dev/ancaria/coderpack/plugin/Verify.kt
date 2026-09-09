package dev.ancaria.coderpack.plugin

import dev.ancaria.coderpack.verify.Level
import dev.ancaria.coderpack.verify.Verifier
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Runs the linter over the packed mod.
 *
 * The library does the work. This is the Gradle half of it. It reads one jar
 * and writes one report, holds nothing but properties, and never touches
 * `project` in its action. This build runs with the configuration cache and
 * `problems=fail`, so a task that did would break it here rather than in
 * somebody's mod build.
 *
 * A failure is thrown with the whole report as its message, which puts every
 * finding under Gradle's "What went wrong" instead of scrolled off the top.
 */
@CacheableTask
abstract class Verify : DefaultTask() {

    /**
     * The jar to read. Only its name reaches the output, so the file changing
     * matters and the path it sits at does not.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val modJar: RegularFileProperty

    /** The findings, kept so the task can be up to date and cached. */
    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val result = Verifier.verify(modJar.get().asFile.toPath())
        val text = result.toString()
        report.get().asFile.apply { parentFile.mkdirs() }.writeText(text + "\n")
        if (!result.ok()) {
            throw GradleException(text)
        }
        // Warnings are worth reading once. A clean run is worth nothing on
        // screen, and `--info` is where somebody who wants it will look.
        if (result.count(Level.WARNING) > 0) logger.lifecycle(text) else logger.info(text)
    }
}
