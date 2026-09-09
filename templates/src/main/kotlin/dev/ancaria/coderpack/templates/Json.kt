package dev.ancaria.coderpack.templates

/**
 * Just enough JSON to write one file.
 *
 * There is no JSON anywhere else in this tool and no library on its classpath,
 * and a registry index is an object of six fields holding an array of objects of
 * strings and numbers. Writing that is thirty lines. Taking a dependency for it
 * would put a serialiser into every mod build that resolves the plugin.
 *
 * Only the writing side exists. Nothing here reads JSON back, because the file
 * this produces is generated and never edited by hand. What a human writes is
 * `registry.toml`, in the format every other descriptor in this project uses.
 */
object Json {

    /** A string as JSON, escaped. */
    fun string(value: String): String {
        val out = StringBuilder("\"")
        for (character in value) {
            when {
                character == '"' -> out.append("\\\"")
                character == '\\' -> out.append("\\\\")
                character == '\n' -> out.append("\\n")
                character == '\r' -> out.append("\\r")
                character == '\t' -> out.append("\\t")
                // Everything below a space has no literal form. Everything above
                // does: this file carries mod descriptions in whatever language
                // they were written in, and \u-escaping them would make the diff
                // unreadable for no gain in a UTF-8 file.
                character < ' ' -> out.append("\\u%04x".format(character.code))
                else -> out.append(character)
            }
        }
        return out.append("\"").toString()
    }

    /** `["a", "b"]` on one line, which is where every array in this file fits. */
    fun strings(values: List<String>): String =
        values.joinToString(", ", "[", "]") { string(it) }

    /**
     * An object, one field per line, at [indent].
     *
     * Fields are `name to already-encoded-value`, and a null value drops the
     * field: an index says nothing about a website a mod does not have, rather
     * than saying it is empty.
     */
    fun obj(indent: String, fields: List<Pair<String, String?>>): String {
        val inner = indent + "  "
        val written = fields.filter { it.second != null }
            .joinToString(",\n") { (name, value) -> "$inner${string(name)}: $value" }
        return "{\n$written\n$indent}"
    }
}
