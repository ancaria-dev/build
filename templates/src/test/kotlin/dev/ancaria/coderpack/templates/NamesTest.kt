package dev.ancaria.coderpack.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The names everything else is derived from, and the one that is refused. */
class NamesTest {

    @Test
    fun `takes an id the plugin would take`() {
        assertEquals("my-mod", Names.id("my-mod"))
        assertEquals("m2", Names.id("m2"))
    }

    @Test
    fun `refuses an id the launcher could not write down`() {
        val failed = assertFailsWith<Fail> { Names.id("My Mod") }
        // The rule comes from the linter, so the message can quote it.
        assertTrue("[a-z0-9]" in failed.message!!, failed.message!!)
        // And a scaffolder that only says no is a scaffolder somebody guesses at.
        assertTrue("my-mod" in failed.message!!, failed.message!!)
        assertFailsWith<Fail> { Names.id("-leading") }
        assertFailsWith<Fail> { Names.id("") }
    }

    @Test
    fun `derives what it can from the id`() {
        assertEquals("My Mod", Names.display("my-mod"))
        assertEquals("mods.mymod", Names.pkg("my-mod"))
        assertEquals("MyMod", Names.klass("my-mod"))
    }

    @Test
    fun `keeps a name a compiler will take when the id starts with a digit`() {
        assertEquals("mods.m9lives", Names.pkg("9lives"))
        assertEquals("Mod9lives", Names.klass("9lives"))
    }

    @Test
    fun `refuses a package rather than repairing it`() {
        assertEquals("dev.example.mod", Names.validPkg("dev.example.mod"))
        assertFailsWith<Fail> { Names.validPkg("9dev.mod") }
        assertFailsWith<Fail> { Names.validPkg("dev example") }
        assertFailsWith<Fail> { Names.validPkg("") }
    }
}
