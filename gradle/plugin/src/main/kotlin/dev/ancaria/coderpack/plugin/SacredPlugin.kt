package dev.ancaria.coderpack.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Packages a directory of code as a mod the Sacred loader can load.
 *
 * Four things happen, and nothing else:
 *
 *  * the descriptor is generated from the `sacred { }` block and added as a
 *    resource, so the jar always describes the jar;
 *  * `verifySacredMod` reads the packed jar and refuses to let a mod the loader
 *    would skip go any further;
 *  * `assembleSacredMod` produces one jar with the mod's dependencies inside it,
 *    because each mod is loaded by its own class loader and there is nowhere
 *    else for a library to come from;
 *  * `installSacredMod` drops that jar wherever the build points it, which
 *    during development is the game's own mods folder.
 *
 * The only language plugin applied here is `java`, which every JVM language
 * plugin builds on. Kotlin, Groovy and Scala mods work by adding their own
 * plugin next to this one; their sources compile into the same jar and this
 * plugin never has to know which one is in use.
 */
class SacredPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply(JavaPlugin::class.java)
        // Applied by id rather than by class: the fat jar is Shadow's job, and
        // going through the id keeps us off its Kotlin API, which changes more
        // often than the task name does.
        target.pluginManager.apply(SHADOW)

        val mod = target.extensions.create<SacredExtension>(EXTENSION)
        mod.displayName.convention(mod.id)
        mod.version.convention(target.provider { target.version.toString() })
        mod.authors.convention(emptyList())
        mod.conflicts.convention(emptyList())
        // The default is this major and no other, which is what every descriptor
        // meant when the field was a bare number. An author who knows their code
        // survives the next one widens it and takes responsibility for saying so.
        mod.apiRange.convention(Descriptor.API)

        // --release rather than a toolchain: the author compiles with whatever
        // JDK they have and the class files still run on the oldest JVM the
        // loader supports. A toolchain would send Gradle downloading a JDK the
        // first time somebody builds a mod.
        target.tasks.withType<JavaCompile>().configureEach {
            options.release.set(JAVA_TARGET)
        }

        // compileOnly on purpose: the loader already has the API on its own
        // classpath, and a second copy inside the mod jar would be a different
        // class with the same name -- a ClassCastException between two things
        // that are obviously the same type. Nothing is added when no version is
        // set, because during development the API is usually a local jar.
        target.dependencies.addProvider(
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            mod.apiVersion.map { "$API_GROUP:api:$it" }
        )

        val descriptor = target.tasks.register<Descriptor>("generateModDescriptor") {
            group = GROUP
            description = "Writes ${Descriptor.DESCRIPTOR} from the sacred block"
            modId.set(mod.id)
            displayName.set(mod.displayName)
            modVersion.set(mod.version)
            modDescription.set(mod.description)
            entrypoint.set(mod.entrypoint)
            authors.set(mod.authors)
            website.set(mod.website)
            repository.set(mod.repository)
            conflicts.set(mod.conflicts)
            apiRange.set(mod.apiRange)
            loaderRange.set(mod.loaderRange)
            outputDirectory.set(target.layout.buildDirectory.dir("generated/sacred-mod"))
        }

        // from(TaskProvider) carries the dependency, so nothing has to remember
        // to run the generator first.
        target.tasks.named<ProcessResources>("processResources") {
            from(descriptor)
        }

        val shadow = target.tasks.named<Jar>("shadowJar") {
            // No classifier: the file that lands in the mods folder is the one
            // named after the mod, not the one with `-all` stuck on the end.
            archiveClassifier.set("")
            // Shadow's transformers merge the files that are meant to appear in
            // several jars at once -- service registrations, and the
            // `.kotlin_module` files a Kotlin mod brings in with the standard
            // library. They only see an entry the copy hands them, and the
            // default strategy drops the second one first, which Shadow itself
            // warns about four times on every Kotlin build.
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            archiveBaseName.set(mod.id.orElse(target.name))
            // Signatures belong to the jars the classes came from and mean
            // nothing once those classes are inside a different jar; leaving
            // them in makes the class loader reject the lot.
            exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
            // Several module descriptors in one jar describe nothing. Mods are
            // loaded from the classpath, where module-info is ignored anyway,
            // and keeping them only invites a tool to take them seriously.
            exclude("module-info.class", "META-INF/versions/*/module-info.class")
        }

        // Hung off the packaging path rather than off `check` alone: a mod
        // author runs installSacredMod far more often than build, and a jar that
        // fails the lint must never reach the mods folder. The check follows the
        // artifact, so nothing is copied anywhere until the jar has passed.
        val verify = target.tasks.register<Verify>("verifySacredMod") {
            group = GROUP
            description = "Checks the packed mod against what the loader requires"
            modJar.set(shadow.flatMap { it.archiveFile })
            report.set(target.layout.buildDirectory.file("reports/sacred-mod/verify.txt"))
        }

        val assemble = target.tasks.register<Copy>("assembleSacredMod") {
            group = GROUP
            description = "Builds the mod jar with its dependencies inside"
            dependsOn(verify)
            from(shadow)
            into(target.layout.buildDirectory.dir("sacred-mod"))
        }

        // And on `check` as well, so `build` and any CI that runs it cover the
        // mod without having to know the task by name.
        target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
            dependsOn(verify)
        }

        target.tasks.register<Copy>("installSacredMod") {
            group = GROUP
            description = "Copies the mod jar into sacred.installTo"
            from(assemble)
            into(mod.installTo.orElse(target.layout.buildDirectory.dir("sacred-mod")))
        }
    }

    private companion object {
        const val EXTENSION = "sacred"
        const val GROUP = "sacred"
        const val SHADOW = "com.gradleup.shadow"
        const val API_GROUP = "dev.ancaria.coderpack"
        const val JAVA_TARGET = 21
    }
}
