plugins {
    groovy
    id("dev.ancaria.coderpack") version "{{plugin}}"
}

version = "{{version}}"

dependencies {
    // Anything this mod needs at run time goes here, and travels inside the
    // jar: every mod is loaded by its own class loader, so a library that is
    // not in the jar is a library the mod cannot see.

    // Groovy is one of those. The loader carries the API and nothing else, so
    // implementation rather than compileOnly: this is the scope the fat jar
    // packs, and the other one compiles and then throws NoClassDefFoundError
    // somewhere inside the game. It is the biggest thing in the jar by far.
    implementation("org.apache.groovy:groovy:{{groovy}}")
}

tasks.withType<GroovyCompile>().configureEach {
    // The loader targets Java 21 and starts on whatever JVM the player has.
    // groovyc reads targetCompatibility, and left to itself that is the JDK
    // running Gradle, so a mod built on a newer one loads here and dies on
    // somebody else's machine. options.release settles the javac half, for a
    // project that later puts a .java file next to its .groovy ones.
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.release = 21
    options.encoding = "UTF-8"
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
