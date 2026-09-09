package dev.ancaria.coderpack.templates

/**
 * Something the person at the keyboard got wrong, said in one line.
 *
 * A stack trace here would be noise: nothing in this tool fails for a reason
 * the caller cannot fix by typing something else. [code] is what the process
 * exits with: 2 for a call that was wrong, which is what a shell script
 * checks for.
 */
class Fail(message: String, val code: Int = 2) : RuntimeException(message)
