package dev.ancaria.coderpack.ktx

/*
 * Stub of dev.ancaria.coderpack:api-kotlin, which coderpack publishes and this
 * repository does not read.
 *
 * The Java half of the same problem is next door, in the linter's fixtures, and
 * it is compiled a second time by this build under the real coordinates. This
 * one has no such source to borrow, because those fixtures are Java. So it is
 * written out here, and what it has to contain is exactly what the Kotlin
 * templates call.
 *
 * Right now that is nothing. A Kotlin mod extends the Java SacredMod directly,
 * and the generated entrypoints reach the API through its getters, which
 * Kotlin already reads as properties. The generated build script still depends
 * on the artifact, so the coordinates have to resolve, and this file is what
 * makes the jar. A template that starts using an extension adds it here too,
 * or the end-to-end test stops compiling, which is the point of it.
 */
