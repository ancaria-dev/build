import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    // The Kotlin the running Gradle already carries, so this module compiles at
    // the same version :plugin does and no Kotlin plugin is downloaded to build
    // a command line tool.
    `embedded-kotlin`
    application
    // The scaffolder is a library as well as a command line. The IDE plugin in
    // ancaria-dev/idea puts the same New Project dialog in front of somebody
    // who never opens a terminal, and it calls Scaffold rather than shipping a
    // second copy of the templates that would drift from these by the second
    // release.
    `maven-publish`
    // Central refuses an unsigned artifact.
    signing
    // Offers this project to the aggregation at the root, which is what
    // actually uploads. The version comes from there.
    id("com.gradleup.nmcp")
}

// The same coordinates and the same version as everything else here: the plugin
// a scaffolded project applies is the plugin this tool was built beside.
group = property("group") as String
version = property("version") as String

// Read once, out here: inside a task block `property` is the task's own.
val apiVersion = property("apiVersion") as String

dependencies {
    // `embedded-kotlin` puts the standard library on the compile classpath only,
    // because a Gradle plugin is handed one at run time. This is not a plugin,
    // it is a script somebody runs, so the same version goes in the
    // distribution as well.
    implementation(embeddedKotlin("stdlib"))
    // The linter, whole. `coderpack verify` is the same library the Gradle task
    // calls, and the scaffolder asks it what a mod id is allowed to be rather
    // than keeping a third copy of that rule.
    implementation(project(":verify"))
    testImplementation(kotlin("test"))
    // The end-to-end test builds a scaffolded mod with a real Gradle.
    testImplementation(gradleTestKit())
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

// No toolchain, for the reason the plugin next door gives: the JDK running
// Gradle compiles, and release settles what comes out of it.
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

application {
    // installDist writes build/install/coderpack/bin/coderpack{,.bat}.
    mainClass = "dev.ancaria.coderpack.templates.Main"
    applicationName = "coderpack"
}

// --- what the tool carries inside it -------------------------------------

/**
 * Writes the list of resources that ships inside the jar beside them.
 *
 * A jar cannot be asked what is in one of its packages, so a scaffolder either
 * knows its templates by name in code or reads an index. This produces the
 * index from the directory, which is what makes a new template (and a new
 * language) a matter of adding files: nothing in `Templates.kt` or
 * `Languages.kt` ever learns a name.
 */
abstract class ResourceIndex : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: DirectoryProperty

    @get:OutputFile
    abstract val index: RegularFileProperty

    @TaskAction
    fun write() {
        val root = source.get().asFile
        val paths = root.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            // A template spells a dotfile with a leading `_` and Scaffold puts
            // the dot back, so nothing here is meant to start with one. What
            // does is somebody's tool leaving scratch behind, a `.gradle`
            // lock file under a `files/` tree, and indexing it would copy it
            // into every project this tool writes.
            .filterNot { path -> path.split('/').any { it.startsWith(".") } }
            .sorted().toList()
        val file = index.get().asFile
        file.parentFile.mkdirs()
        file.writeText(paths.joinToString("\n", postfix = "\n"))
    }
}

// Written next to the classes rather than at the root of the jar, so nothing
// else on the distribution's classpath can shadow it with an index of its own.
val resourceIndex = tasks.register<ResourceIndex>("resourceIndex") {
    description = "Lists the templates and languages for the tool to read at run time"
    source = layout.projectDirectory.dir("src/main/resources")
    index = layout.buildDirectory.file(
        "generated/index/dev/ancaria/coderpack/templates/index.txt"
    )
}

// The wrapper a scaffolded project gets is this repository's own, staged as a
// resource rather than checked in a second time: the distribution a generated
// mod downloads is then the one this build is tested with, and there is no
// second gradle-wrapper.jar to keep in step.
val wrapperResources = tasks.register<Copy>("wrapperResources") {
    description = "Stages this build's Gradle wrapper as a resource of the tool"
    into(layout.buildDirectory.dir("generated/wrapper/wrapper"))
    from(rootDir.resolve("gradlew"))
    from(rootDir.resolve("gradlew.bat"))
    from(rootDir.resolve("gradle/wrapper")) { into("gradle/wrapper") }
}

// Written by the build so the tool cannot name a plugin version nobody
// published. `api` is the artifact version of dev.ancaria.coderpack:api, which
// moves on its own and is not the api = "1" line in a descriptor.
val versionResource = tasks.register<WriteProperties>("versionResource") {
    description = "Records the versions a scaffolded project is written with"
    destinationFile = layout.buildDirectory.file(
        "generated/version/dev/ancaria/coderpack/templates/coderpack.properties"
    )
    property("tool", version.toString())
    property("plugin", version.toString())
    property("api", apiVersion)
}

sourceSets["main"].resources.srcDirs(
    layout.buildDirectory.dir("generated/index"),
    layout.buildDirectory.dir("generated/version"),
    layout.buildDirectory.dir("generated/wrapper"),
)

tasks.named<ProcessResources>("processResources") {
    dependsOn(resourceIndex, versionResource, wrapperResources)
}

// --- published beside the plugin and the linter ---------------------------

// Same coordinates, same version, same repository as the other two. A consumer
// resolves `dev.ancaria.coderpack:templates` and gets the templates, the
// languages, the build DSLs and the Gradle wrapper inside the jar, which is the
// whole of what `coderpack new` writes minus the argument parsing.
java {
    withSourcesJar()
    // Empty, because the sources here are Kotlin and javadoc reads Java. It
    // exists because Central refuses a publication without one.
    withJavadocJar()
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

// The generated resources are resources, so the sources jar packs them too, and
// it has to wait for the tasks that write them. Without this Gradle refuses the
// build rather than racing: three generated source directories, three
// dependencies nothing else declares.
tasks.named<Jar>("sourcesJar") {
    dependsOn(resourceIndex, versionResource, wrapperResources)
}

publishing {
    // No repository block. Central is not a repository a publish task writes
    // to. The Portal takes one signed bundle over its own API, and that is
    // what the aggregation at the root does.
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "Sacred mod scaffolder"
                description = "Writes a Sacred Gold mod project: templates, languages " +
                    "and build DSLs, with the Gradle wrapper inside"
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

// --- the end-to-end test needs an API to compile a mod against ------------

// The loader API is published by coderpack, which this repository does not read
// and CI does not check out. The linter's fixtures already carry a stub of it,
// so it is compiled a second time here and packed under the real coordinates:
// one stub of the API in this repository rather than two that drift apart.
val apiStub = sourceSets.create("apiStub") {
    java.setSrcDirs(listOf(file("../verify/src/fixtures/java")))
    java.include("dev/ancaria/coderpack/api/**")
}

val apiStubJar = tasks.register<Jar>("apiStubJar") {
    group = "verification"
    description = "Packs the stub loader API for a scaffolded mod to resolve"
    archiveFileName = "api-$apiVersion.jar"
    destinationDirectory = layout.buildDirectory.dir("api-stub")
    from(apiStub.output)
}

// The Kotlin half of the same problem. A generated Kotlin mod also depends on
// dev.ancaria.coderpack:api-kotlin, which coderpack publishes as well. There is
// no source to borrow for this one, because the linter's fixtures are Java, so
// it is written out under src/apiKotlinStub and holds exactly the declarations
// the Kotlin templates call.
val apiKotlinStub = sourceSets.create("apiKotlinStub")

dependencies {
    // The stub API it is written against, and the standard library it is
    // written in. Both compileOnly: this jar carries neither, the same way the
    // published module carries neither.
    "apiKotlinStubCompileOnly"(apiStub.output)
    "apiKotlinStubCompileOnly"(embeddedKotlin("stdlib"))
}

val apiKotlinStubJar = tasks.register<Jar>("apiKotlinStubJar") {
    group = "verification"
    description = "Packs the stub Kotlin API for a scaffolded Kotlin mod to resolve"
    archiveFileName = "api-kotlin-$apiVersion.jar"
    // The same directory as the jar above. The test offers one flatDir
    // repository and both coordinates have to be findable in it.
    destinationDirectory = layout.buildDirectory.dir("api-stub")
    from(apiKotlinStub.output)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(apiStubJar, apiKotlinStubJar)
    // The end-to-end test resolves the plugin the way a mod author does, out of
    // the local Maven repository, so both halves of it have to be there first.
    dependsOn(":plugin:publishToMavenLocal", ":verify:publishToMavenLocal")
    inputs.dir(layout.buildDirectory.dir("api-stub")).withPropertyName("apiStub")
    systemProperty(
        "coderpack.apiStub",
        layout.buildDirectory.dir("api-stub").get().asFile.path
    )
    systemProperty("coderpack.apiVersion", apiVersion)
}
