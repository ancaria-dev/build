// Nothing to build at the root. The plugin is the product.
//
// What is here is the list of what leaves this repository, and the two halves
// of it go to different places for different reasons. The plugin goes to the
// Gradle Plugin Portal, because `id("dev.ancaria.coderpack")` in a mod's build
// script is resolved from there and from nowhere else. The linter and the
// scaffolder go to Maven Central: the first because the plugin's own POM names
// it and a mod build has to be able to fetch it, the second because the IDE
// plugin in ancaria-dev/idea depends on it.
plugins {
    id("com.gradleup.nmcp.aggregation") version "1.6.2"
}

dependencies {
    nmcpAggregation(project(":verify"))
    nmcpAggregation(project(":templates"))
}

nmcpAggregation {
    centralPortal {
        username = providers.environmentVariable("CENTRAL_USERNAME")
        password = providers.environmentVariable("CENTRAL_PASSWORD")

        // The upload is automatic; the release is one click in the portal. A
        // Central artifact can never be deleted, so the first few releases are
        // worth looking at before they are permanent. Change this to
        // "AUTOMATIC" once the shape of a deployment is known to be right, and
        // raising a version number is again the only thing that ships.
        publishingType = "USER_MANAGED"
        publicationName = "${property("group")}:${property("version")}"
    }
}
