package dev.ancaria.coderpack.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * What a mod says about itself.
 *
 * ```kotlin
 * sacred {
 *     id = "self-check"
 *     displayName = "Self Check"
 *     description = "Exercises every event the loader can deliver"
 *     entrypoint = "dev.ancaria.selfcheck.SelfCheckMod"
 *     authors = listOf("MairwunNx (Pavel Erokhin)")
 *     website = "https://ancaria.dev"
 *     repository = "https://github.com/ancaria-dev/mods"
 * }
 * ```
 *
 * Everything here ends up in the descriptor inside the jar, which is the only
 * thing the launcher and the loader read before deciding to run any of it.
 */
abstract class SacredExtension {

    /** Handle: lowercase, digits and hyphens. This is what gets ticked in the launcher. */
    abstract val id: Property<String>

    /** The name a player reads. Spaces and capitals, not the id. */
    abstract val displayName: Property<String>

    /** One sentence, shown under the name in the mod list. */
    abstract val description: Property<String>

    /** Defaults to the project version, so a mod that sets `version` is already done. */
    abstract val version: Property<String>

    /** The class implementing `SacredMod`. The loader instantiates exactly this one. */
    abstract val entrypoint: Property<String>

    abstract val authors: ListProperty<String>

    abstract val website: Property<String>

    /** Where the source lives. A player who wants to read it should not have to search. */
    abstract val repository: Property<String>

    /**
     * Mods this one cannot be installed beside, by id.
     *
     * Two mods that rewrite the same pickup produce whichever answer the loader
     * happened to ask for last, and a player has no way to see that is what
     * happened. Naming the other mod here is what turns that into a rule: the
     * launcher hides both sides rather than offering a choice nobody can make
     * an informed decision about.
     *
     * It travels in the descriptor rather than only in a registry index, so it
     * still holds for a jar somebody dropped into the folder by hand.
     */
    abstract val conflicts: ListProperty<String>

    /**
     * Which API contracts this mod runs on, as a range: `[1,2)` is this major
     * and no other, `[1,3)` is this one and the next, `[1,)` is this one and
     * everything after it.
     *
     * Left alone it is `[<current>,<current+1>)`, which is what a mod meant back
     * when the field was a bare number, and it is the right answer for almost
     * every mod: the contract changes major exactly when something a mod calls
     * stops being there.
     *
     * Widening it is a promise, and it is the author's to make -- they are the
     * only one who knows whether their code stays inside the part of the API
     * that did not move. What it cannot do is claim a contract this toolchain
     * did not compile against: `verifySacredMod` fails a jar whose range does
     * not contain the API the build resolved, because a version stamped by
     * somebody with no way to check it is worse than no version at all.
     *
     * The notation is Maven's, the one NeoForge writes; `Ranges` in the linter
     * documents it.
     */
    abstract val apiRange: Property<String>

    /**
     * Which Sacred Mod Loader releases this mod wants, as a range.
     *
     * Unset, and nothing is written: most mods have no opinion about the
     * launcher's version number and should not have to invent one. Set it when
     * the mod needs something a particular release added -- `[0.2.0,)` -- and
     * the launcher will list the mod, refuse to enable it, and say which release
     * it asked for.
     *
     * Nothing here can check it. Launcher releases are somebody else's numbers
     * and the ones that matter have usually not happened yet, so the linter
     * checks the syntax and stops there.
     */
    abstract val loaderRange: Property<String>

    /**
     * Adds the loader API as a compile-only dependency when set. Left alone, the
     * build declares its own -- a local jar works exactly as well as a
     * coordinate, and during development it is usually a local jar.
     */
    abstract val apiVersion: Property<String>

    /**
     * Where `installSacredMod` copies the finished jar, normally
     * `<Sacred Gold>/mods`. Unset, the jar stays in the build directory.
     */
    abstract val installTo: DirectoryProperty

    /** Reads better than `authors.add(...)` for the common case of one person. */
    fun author(name: String) {
        authors.add(name)
    }

    /** Same, for the common case of one incompatible mod. */
    fun conflictsWith(id: String) {
        conflicts.add(id)
    }
}
