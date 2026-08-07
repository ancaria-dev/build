pluginManagement {
    repositories {
        // Nothing is released yet, so both halves of the toolchain come from
        // your own machine: `./gradlew publishToMavenLocal` in a checkout of
        // ancaria-dev/build puts the plugin here, and the same command in a
        // checkout of ancaria-dev/coderpack puts the API here. The portal and
        // Central below are where each will come from after the first release;
        // until then they resolve nothing and cost one lookup.
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "{{id}}"
