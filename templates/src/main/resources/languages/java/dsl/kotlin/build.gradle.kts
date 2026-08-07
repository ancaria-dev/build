plugins {
    id("dev.ancaria.coderpack") version "{{plugin}}"
}

version = "{{version}}"

dependencies {
    // Anything this mod needs at run time goes here, and travels inside the
    // jar: every mod is loaded by its own class loader, so a library that is
    // not in the jar is a library the mod cannot see.
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
