package dev.ancaria.coderpack.ktx

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.api.Events
import dev.ancaria.coderpack.api.Handle
import dev.ancaria.coderpack.api.Priority
import dev.ancaria.coderpack.api.event.Event
import dev.ancaria.coderpack.api.event.Hero
import dev.ancaria.coderpack.api.SacredMod as ApiMod

import java.util.function.Consumer

/*
 * Stub of dev.ancaria.coderpack:api-kotlin, which coderpack publishes and this
 * repository does not read.
 *
 * The Java half of the same problem is next door, in the linter's fixtures, and
 * it is compiled a second time by this build under the real coordinates. This
 * one has no such source to borrow, because those fixtures are Java. So it is
 * written out here, and what it has to contain is exactly what the Kotlin
 * templates call: the signatures below and no more. A template that starts
 * using another extension adds it here too, or the end-to-end test stops
 * compiling, which is the point of it.
 *
 * Only the shapes matter. Nothing here runs: the test builds a mod jar and
 * reads it with the linter, which never loads a class.
 */

/** The abstract mod: keeps the context, hands it to [load] as a receiver. */
public abstract class SacredMod : ApiMod {

    public lateinit var context: Context
        private set

    final override fun onLoad(context: Context) {
        this.context = context
        context.load()
    }

    protected abstract fun Context.load()
}

/** The event type as a type argument rather than a class literal. */
public inline fun <reified E : Event> Events.on(
    priority: Priority = Priority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline listener: (E) -> Unit,
): Handle = on(E::class.java, priority, ignoreCancelled, Consumer { listener(it) })

/** The registration block, with [Events] as the receiver inside. */
public inline fun Context.events(block: Events.() -> Unit) {
    events().block()
}

/** One of the many event properties, and the one the `mod` template reads. */
public inline val Hero.level: Int get() = level()
