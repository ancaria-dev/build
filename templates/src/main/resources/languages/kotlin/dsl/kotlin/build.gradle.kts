import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "{{kotlin}}"
    id("dev.ancaria.coderpack") version "{{plugin}}"
}

version = "{{version}}"

dependencies {
    // Anything this mod needs at run time goes here, and travels inside the
    // jar: every mod is loaded by its own class loader, so a library that is
    // not in the jar is a library the mod cannot see.

    // The standard library is one of those. The loader carries the API and
    // nothing else, so implementation rather than compileOnly: this is the
    // scope the fat jar packs, and the other one compiles and then throws
    // NoClassDefFoundError somewhere inside the game.
    implementation(kotlin("stdlib"))
}

kotlin {
    compilerOptions {
        // The loader targets Java 21 and starts on whatever JVM the player has.
        // Left to itself the Kotlin compiler emits class files for the JDK that
        // happens to be running Gradle, so a mod built on a newer one loads
        // here and dies on somebody else's machine.
        jvmTarget = JvmTarget.JVM_21
    }
}

sacred {
    id = "{{id}}"
    displayName = "{{name}}"
    description = "{{description}}"
    entrypoint = "{{entrypoint}}"
    author("{{author}}")

    // The loader hands the mod this API, so it is compiled against and never
    // packed. A second copy inside the jar is a different class with the same
    // name, and the first symptom is a ClassCastException between two types
    // that are obviously identical.
    apiVersion = "{{api}}"

    // Where installSacredMod drops the finished jar. Pass the game folder on
    // the command line rather than writing your own path into the build:
    //   gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
