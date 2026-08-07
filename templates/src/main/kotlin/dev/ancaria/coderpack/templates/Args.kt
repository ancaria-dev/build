package dev.ancaria.coderpack.templates

/**
 * `--key value`, `--key=value`, bare flags, and everything else positional.
 *
 * Small on purpose. A scaffolder with nine options does not need an argument
 * library, and an unknown option is refused rather than ignored: a typo that
 * silently keeps the default is a project written wrong.
 *
 * `--help` and `-h` are handled here rather than in each subcommand, because
 * that is the one place every option goes past and because a tool that refuses
 * the first thing everybody types has taught the wrong lesson already.
 */
class Args private constructor(
    private val values: Map<String, String>,
    private val flags: Set<String>,
    /** The positional arguments, in the order they were typed. */
    val rest: List<String>,
) {

    fun option(name: String): String? = values[name]

    fun flag(name: String): Boolean = name in flags

    companion object {

        fun parse(
            argv: List<String>,
            options: Set<String>,
            flags: Set<String>,
            help: String,
        ): Args {
            val values = mutableMapOf<String, String>()
            val given = mutableSetOf<String>()
            val rest = mutableListOf<String>()
            var at = 0
            while (at < argv.size) {
                val argument = argv[at]
                if (argument == "--help" || argument == "-h") {
                    throw Help(help)
                }
                if (!argument.startsWith("-")) {
                    rest += argument
                    at++
                    continue
                }
                val body = argument.removePrefix("--")
                val name = body.substringBefore('=')
                val inline = if ('=' in body) body.substringAfter('=') else null
                when (name) {
                    in flags -> {
                        if (inline != null) throw Fail("--$name doesn’t take a value")
                        given += name
                    }
                    in options -> values[name] = inline ?: next(argv, ++at, name)
                    else -> throw Fail("There’s no “$argument” option.\n\n" + help)
                }
                at++
            }
            return Args(values, given, rest)
        }

        /** The next argument, unless it is the next option and the value is missing. */
        private fun next(argv: List<String>, at: Int, name: String): String {
            val value = argv.getOrNull(at)
            if (value == null || value.startsWith("--")) {
                throw Fail("--$name requires a value")
            }
            return value
        }
    }
}
