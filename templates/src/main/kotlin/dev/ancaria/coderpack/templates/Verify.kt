package dev.ancaria.coderpack.templates

import dev.ancaria.coderpack.verify.Verifier
import java.nio.file.Files
import java.nio.file.Path

/**
 * `coderpack verify <jar>`: the linter, on a path, with no Gradle in the
 * picture.
 *
 * The library does all of it. This reads the arguments and turns the report
 * into an exit code -- 1 when a jar carries an error, so a CI job needs no
 * `if` around it. Warnings are printed and change nothing: a release stops for
 * a jar that will not load, not for one that will.
 */
object Verify {

    fun usage(): String = "Usage: coderpack verify <mod jar> [<mod jar> …]"

    fun run(argv: List<String>): Int {
        // No options of its own, but the arguments still go through the parser:
        // that is where --help lives, and where a misspelt flag is refused
        // instead of being looked for on disk as a file name.
        val args = Args.parse(argv, emptySet(), emptySet(), usage())
        if (args.rest.isEmpty()) {
            throw Fail(usage())
        }
        var broken = false
        for (argument in args.rest) {
            val jar = Path.of(argument)
            if (!Files.isRegularFile(jar)) {
                throw Fail("$argument: file not found.")
            }
            val report = Verifier.verify(jar)
            println(report)
            broken = broken || !report.ok()
        }
        return if (broken) 1 else 0
    }
}
