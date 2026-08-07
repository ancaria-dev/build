package dev.ancaria.coderpack.templates

import kotlin.system.exitProcess

/**
 * `coderpack`, the one command a mod author runs.
 *
 * Subcommands rather than a binary each: starting a mod and checking a mod are
 * the same job a day apart, and a second executable on the PATH is a second
 * thing to find, install and keep in step.
 */
object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val rest = args.drop(1)
        val code = try {
            when (args.firstOrNull()) {
                "new" -> New.run(rest)
                "verify" -> Verify.run(rest)
                "index" -> Registry.run(rest)
                "templates" -> say(catalogue())
                "version", "--version", "-v" -> say(version())
                null, "help", "--help", "-h" -> say(usage())
                else -> throw Fail("There’s no “${args[0]}” command.\n\n" + usage())
            }
        } catch (help: Help) {
            // Asking is not failing: it goes to stdout and the process exits 0.
            say(help.text)
        } catch (fail: Fail) {
            System.err.println("coderpack: " + fail.message)
            fail.code
        }
        exitProcess(code)
    }

    /** `coderpack templates`: every axis a project is written along, and what each is. */
    private fun catalogue() = listOf(Templates.list(), Languages.list(), Dsls.list())
        .joinToString("\n\n")

    private fun say(text: String): Int {
        println(text)
        return 0
    }

    private fun version() = "coderpack " + Versions.tool + " writes projects with plugin " +
        Versions.plugin + " and API " + Versions.api

    /** The one copy of the option list is [New.HELP]; this frames it. */
    private fun usage() = """
        Coderpack tools for Sacred Gold mods

        Usage:
          coderpack new <name> [options]      write a project for a new mod
          coderpack verify <jar> [<jar> …]    check a packed mod jar
          coderpack index [<repository>]      write the registry index a launcher reads
          coderpack templates                 what new can write, and in what
          coderpack version                   the versions a project is written with

        Options for new:
    """.trimIndent() + "\n" + New.HELP
}
