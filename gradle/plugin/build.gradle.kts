plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    // Publishes to the Gradle Plugin Portal, which is the only place
    // `id("dev.ancaria.coderpack")` in a mod build script resolves from. It
    // reads the gradlePlugin block below, which was already written for it.
    id("com.gradle.plugin-publish") version "2.1.1"
}

// Both from gradle.properties: CI reads the version out of that file to
// decide whether this build is a new one worth publishing.
group = property("group") as String
version = property("version") as String

dependencies {
    implementation(libs.shadow)
    // The linter, which is a library first: the same code runs from CI with no
    // Gradle in the picture. It ships with the plugin because verifySacredMod
    // calls it, and it is published beside the plugin for the same reason.
    implementation(project(":verify"))
    testImplementation(kotlin("test"))
}

// No toolchain block anywhere in this build: a toolchain that is not installed
// sends Gradle looking for one to download, and a build tool that needs the
// network to compile a hello-world is a bad first impression. The JDK running
// Gradle compiles it, and the targets below settle what it produces -- without
// them a JDK 25 build emits class files that CI's Temurin 21 cannot read.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

gradlePlugin {
    website = "https://ancaria.dev"
    vcsUrl = "https://github.com/ancaria-dev/build"
    plugins {
        create("sacred") {
            id = "dev.ancaria.coderpack"
            implementationClass = "dev.ancaria.coderpack.plugin.SacredPlugin"
            displayName = "Sacred mod packaging"
            description = "Writes the mod descriptor and packs a mod with its " +
                "dependencies inside, ready to drop into the mods folder"
            tags = listOf("sacred", "modding", "coderpack")
        }
    }
}

// Sources and javadoc travel with the plugin: somebody reading `sacred { }` in
// their IDE should get the doc comment that explains each field rather than a
// decompiled signature.
java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    // No repository block. The portal is not a Maven repository a publish task
    // writes to: `publishPlugins` uploads there, and `publishToMavenLocal` is
    // what a mod on this machine resolves while a release is being prepared.
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Sacred mod packaging"
            description = "Gradle plugin that packages a Sacred Gold mod"
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
                developerConnection = "scm:git:ssh://git@github.com/ancaria-dev/build.git"
            }
        }
    }
}

// TestKit runs a real Gradle build against a real plugin classpath, which is the
// only way to find out whether the task actually produces a jar a loader accepts.
tasks.test {
    useJUnitPlatform()
    // A functional test starts a second Gradle, and that one needs to see this
    // plugin's dependencies too.
    dependsOn(tasks.named("pluginUnderTestMetadata"))
}
