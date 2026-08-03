plugins {
    `java-library`
    `maven-publish`
    // Central refuses an unsigned artifact.
    signing
    // Offers this project to the aggregation at the root, which is what
    // actually uploads. The version comes from there.
    id("com.gradleup.nmcp")
}

// The same coordinates and the same version as the plugin next door, which
// depends on this jar: a mod build resolves the two together or not at all.
group = property("group") as String
version = property("version") as String

dependencies {
    // ASM reads a class file without loading it. Nothing else in this build has
    // it -- Gradle keeps its own copy relocated where a plugin cannot reach it,
    // and Shadow does not put one on our classpath -- so it is declared here and
    // travels to the plugin as a transitive dependency.
    implementation(libs.asm)
}

// No toolchain, for the reason the plugin gives: the JDK running Gradle
// compiles, and release settles what comes out of it.
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

// There is no application block here any more. The command line half of this
// library is a subcommand of `coderpack` now, in :templates, so a mod author has
// one binary rather than one per tool. The library is unchanged: Verifier is
// still the whole surface, and both callers still go through it.

// Fixture code, compiled by javac like anything else: a mod, a stub of the API
// it is written against, and a handful of classes that each get one rule wrong.
// The tests pack these class files into jars; nothing here ships. The API half
// of it is compiled a second time by :templates, which packs it as an api jar
// for the scaffolded mod in its end-to-end test to compile against -- one stub
// of the API in this repository rather than two that drift.
val fixtures = sourceSets.create("fixtures")

// The one fixture that breaks no rule, packed the way the plugin packs a mod --
// its own classes and the descriptor, and no API. CI lints this jar with the
// command line entry point, which is the half of the library the Gradle task
// never exercises.
tasks.register<Jar>("demoModJar") {
    group = "verification"
    description = "Packs the passing fixture as a mod jar for the command line to lint"
    archiveFileName = "demo-mod.jar"
    destinationDirectory = layout.buildDirectory.dir("demo")
    from(fixtures.output) {
        include("demo/DemoMod.class")
        include("META-INF/declaration.toml")
    }
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
        }
    }
}

tasks.test {
    dependsOn(tasks.named(fixtures.classesTaskName))
    inputs.files(fixtures.output).withPropertyName("fixtures")
    systemProperty("verify.fixtures", fixtures.output.classesDirs.asPath)
}

// Sources so an IDE can step into the linter from a failing build, and javadoc
// because Central refuses a publication without one. There is nothing to read
// in it that the source does not say better.
java {
    withSourcesJar()
    withJavadocJar()
}

// A doclint run listing every method without a comment is not what this jar is
// for, and it would fail the build over it.
tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// Without a key the sign tasks are skipped rather than failing, so a checkout
// with no secrets still builds, tests and publishes to the local repository.
val signingKey = providers.environmentVariable("SIGNING_KEY")

signing {
    isRequired = signingKey.isPresent
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), providers.environmentVariable("SIGNING_PASSWORD").get())
    }
    sign(publishing.publications)
}

publishing {
    // No repository block. Central is not a repository a publish task writes
    // to -- the Portal takes one signed bundle over its own API, and that is
    // what the aggregation at the root does.
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "Sacred mod verifier"
                description = "Checks a built Sacred Gold mod jar against what the " +
                    "loader requires of it"
                url = "https://github.com/ancaria-dev/build"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "mairwunnx"
                        name = "MairwunNx (Pavel Erokhin)"
                        url = "https://ancaria.dev"
                    }
                }
                scm {
                    url = "https://github.com/ancaria-dev/build"
                    connection = "scm:git:https://github.com/ancaria-dev/build.git"
                    developerConnection =
                        "scm:git:ssh://git@github.com/ancaria-dev/build.git"
                }
            }
        }
    }
}
