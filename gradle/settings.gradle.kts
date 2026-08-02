pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "coderpack-build"

include("plugin")

// The linter is a library first and a Gradle task second, so it sits beside the
// build systems rather than inside one of them: CI runs it on a jar with no
// Gradle in the picture. This build compiles it because the plugin depends on
// it, and because one `./gradlew build` should test both halves.
include("verify")
project(":verify").projectDir = file("../verify")

// The scaffolder, and the one command a mod author runs. It lives outside this
// directory for the same reason the linter does: its templates and its jar are
// not part of any build system, and it happens to be built by this one.
include("templates")
project(":templates").projectDir = file("../templates")
