package dev.ancaria.coderpack.templates

/**
 * Somebody asked what a command does.
 *
 * Not a failure, which is why it is not a [Fail]: it goes to standard output
 * and the process exits 0, so `coderpack new --help | less` behaves and a CI
 * job that asks does not go red. Thrown from the argument parser rather than
 * checked in each subcommand, because `--help` has to work everywhere an option
 * does and the parser is the one place that sees every option.
 */
class Help(val text: String) : RuntimeException(text)
