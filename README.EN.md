<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Shadow](https://img.shields.io/badge/Shadow-9.6.1-1E88E5?style=for-the-badge)](https://gradleup.com/shadow/)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[Русский](README.md) · [Deutsch](README.DE.md)

</div>

# build

This repository contains build support for Sacred Gold mods.

`gradle/` holds the `dev.ancaria.coderpack` Gradle plugin. `maven/` contains a
design note for a possible Maven plugin, with no implementation yet.

`verify/` is the linter for packed mod jars. The Gradle plugin runs it before a
jar can be copied, and CI can run the same checks without Gradle.

`templates/` builds the `coderpack` command line. It creates mod projects,
checks jars, and generates the index published by a mod repository. The same
module is published as `dev.ancaria.coderpack:templates` for the IntelliJ IDEA
plugin.

## Start a mod

```
coderpack new my-mod
cd my-mod
gradlew assembleSacredMod
```

The last command writes a jar that the loader can accept. You need a JDK, but
you do not need to copy another project's build file or install a matching
system version of Gradle.

Each release includes `coderpack` as a zip. To build it from a checkout:

```
cd gradle
./gradlew :templates:installDist
../templates/build/install/coderpack/bin/coderpack new my-mod
```

The default project contains:

```
.gitignore
README.md
build.gradle.kts                     the filled sacred { } block
settings.gradle.kts                  mavenLocal first, then the plugin portal
registry.toml                        metadata for a launcher repository
.github/workflows/build.yml          builds and releases each new version
dependencies.json                    the coderpack version CI downloads
gradlew, gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/mods/mymod/MyMod.java  onLoad and one working listener
```

Java is the default language. `--language kotlin` writes
`src/main/kotlin/mods/mymod/MyMod.kt` and configures the Kotlin plugin.
`--language groovy` does the same for Groovy.

A Kotlin project gets one dependency the other two do not:
`dev.ancaria.coderpack:api-kotlin`, the loader API said in Kotlin. Its
entrypoint extends the `SacredMod` class from that module, which keeps the
context and hands it to `Context.load()` as a receiver, and registers its
listener as `on<Hero> { }` rather than as an annotated method. It adds no
capability: every declaration in it forwards to the Java API, and a field the
API lets a listener rewrite is a `var`, so a rewrite reads `it.delta *= 2`.
Deleting the dependency and writing `@Subscribe` instead is a supported way to
have a Kotlin mod. The module is `implementation`, not `compileOnly`, because
the loader hands a mod the API and not this.

The build script language is a separate choice. `--dsl groovy` writes
`build.gradle` and `settings.gradle` instead of the `.kts` files. Without that
option, the project uses Kotlin DSL. Any supported mod language can be paired
with either build DSL.

`--no-registry` omits `registry.toml`, `dependencies.json`, and the release
workflow. By default, a new mod is also a repository that a launcher can
subscribe to.

`my-mod` is the mod id. `coderpack` validates it before writing anything, then
derives `My Mod`, `mods.mymod`, and `MyMod` for the display name, package, and
entrypoint class.

| Option | What it changes |
|---|---|
| `--package dev.example.mymod` | The Java package and entrypoint directory |
| `--display-name "My Splendid Mod"` | The name shown to players |
| `--author "Your Name"` | The descriptor's `authors` value. The current account is used by default |
| `--description "One sentence"` | The text shown below the mod name |
| `--mod-version 0.2.0` | The mod version. The default is `1.0.0` |
| `--dir somewhere/else` | The output directory. The default is `./<name>` |
| `--template minimal` | The project template |
| `--language kotlin` | The mod language: `java`, `kotlin`, or `groovy`. The default is `java` |
| `--dsl groovy` | The build DSL: `kotlin` or `groovy`. The default is `kotlin` |
| `--repo https://github.com/me/my-mod` | The future project URL used to build download links |
| `--no-registry` | Omits `registry.toml`, `dependencies.json`, and the release workflow |
| `--git` | Runs `git init` and adds `--repo` as `origin` |
| `--force` | Writes into a non-empty directory |

The generated project includes the Gradle wrapper and its jar. This keeps the
first build to one command and avoids depending on whichever Gradle version is
installed on the machine. The scaffolder packages this repository's own
wrapper files as resources, so generated projects use Gradle 9.7.1 and verify
the same SHA-256 checksum as this build.

### Make the mod installable

A generated project is also an SRML mod repository unless you pass
`--no-registry`.

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
gradlew assembleSacredMod
coderpack index
```

Commit the generated `sacred.mods.repository.json`, then push. The included
GitHub Actions workflow builds the mod, checks that the committed index still
matches the jar, and creates a release tagged `<id>-v<version>` when that tag
does not exist. A push without a new version creates no release. The workflow
uses GitHub's repository token, so it needs no extra secret.

`coderpack index` reads `registry.toml` and the descriptor inside each built
jar, then writes `sacred.mods.repository.json`. Each mod entry can carry its
name, description, version, API and loader ranges, authors, website, conflicts,
source path, and icon. The distribution block records the file name, size,
SHA-256 hash, and release URL. The root object's `srml` field is `1`, alongside the
repository name, description, URL, icon, and mod array. The output has no
timestamp, so `coderpack index --check` can compare it byte for byte for one
set of jars. The generated workflow does not run that comparison: on the
default branch it regenerates the index from the jar it just built and commits
it, because a mod jar carrying a language runtime is not reproducible from one
machine to the next, and comparing it fails on correct work.

`registry.toml` is the hand-maintained part:

```toml
name = "My Mod"
description = "My Mod, a mod for Sacred Gold"
url = "https://github.com/me/my-mod"
releases = "https://github.com/me/my-mod/releases/download/{id}-v{version}/{file}"
```

`name`, `url`, and `releases` are required. `description` is optional, as is an
`icon` field for a repository icon. The `releases` template builds each download
URL and must contain `{file}`. `coderpack new --repo <url>` fills both URL
fields. Without `--repo`, it writes `https://github.com/you/my-mod` and tells
you which line to change.

`--git` runs after every file has been written. If Git is unavailable, the
project remains usable and the report says that no repository was created.
The scaffolder does not make the first commit.

### Templates, languages, and build scripts

```
coderpack templates
templates:
  minimal   An entrypoint and nothing else. For a mod that listens to nothing.
            groovy, java, kotlin
  mod       An entrypoint and one working listener. The default.
            groovy, java, kotlin

languages:
  groovy    Groovy, with the runtime packed into the jar.
  java      Plain Java. What new writes when nobody says otherwise.
  kotlin    Kotlin, with the standard library packed into the jar.

build scripts:
  groovy    Groovy DSL. build.gradle, the older syntax most Gradle answers are written in.
  kotlin    Kotlin DSL. build.gradle.kts, with completion and refactoring in the IDE.
```

Templates live under
`templates/src/main/resources/templates/<name>/`. Each directory has a
`template.properties`, files shared by every supported language, and one
`lang/<language>/` tree per entrypoint implementation.

Languages live under
`templates/src/main/resources/languages/<name>/`. Each has a
`language.properties`, optional files shared by both build DSLs, and one build
script under `dsl/<dsl>/` for each supported DSL.

Build DSLs live under `templates/src/main/resources/dsl/<name>/`. Each has a
`dsl.properties` and the matching settings file.

This split keeps each varying file in one place. Entrypoints vary by template
and language. Build scripts vary by language and DSL. Settings files vary only
by DSL. The README, `.gitignore`, and repository files are shared. The current
layout produces 12 project combinations from 26 template files instead of
maintaining a separate directory for every combination.

The scaffolder substitutes `{{id}}`, `{{name}}`, `{{description}}`,
`{{version}}`, `{{package}}`, `{{packagePath}}`, `{{class}}`,
`{{entrypoint}}`, `{{author}}`, `{{repo}}`, `{{plugin}}`, and `{{api}}` in
file names and contents. Keys from `language.properties` and `dsl.properties`,
except `description`, become placeholders too. An unresolved placeholder stops
the run.

Adding a template, language, or DSL means adding its resource directory. The
build indexes those resources automatically, so their names are not hardcoded
in Kotlin. Use `_gitignore` for a template file that should become
`.gitignore`, because resource copying and indexing skip dotfiles.

Unsupported languages and DSLs fail before the first file is written:

```
coderpack: there is no language "rust". There is: groovy, java, kotlin
```

`--help` and `-h` work on every subcommand. `coderpack new --help` prints the
same option list included in `coderpack help`.

### Check a jar

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

This runs the same library as `verifySacredMod`, without Gradle. It prints one
block per jar, exits `1` when it finds an error, and exits `2` for invalid
arguments.

## Write a mod

`coderpack new` writes the settings and build files shown below.

`settings.gradle.kts` declares the plugin and API repositories:

```kotlin
pluginManagement {
    repositories {
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

rootProject.name = "my-mod"
```

`mavenLocal()` comes first because local development publishes both the plugin
and API there. No public release exists yet, so build this repository and
`coderpack`, then run `./gradlew publishToMavenLocal` in both checkouts. The
plugin portal and Maven Central are already declared for future releases.

A Kotlin DSL build for a mod looks like this:

```kotlin
plugins {
    id("dev.ancaria.coderpack") version "0.100.0"
}

version = "1.0.0"

dependencies {
    // Anything the mod needs at runtime. It travels inside the jar.
    implementation("org.jetbrains:annotations:26.0.2")
}

sacred {
    id = "my-mod"                                     // lowercase, digits, hyphens
    displayName = "My Mod"                            // what a player reads
    description = "One sentence, shown under the name in the mod list"
    version = "1.0.0"                                 // defaults to the project version
    entrypoint = "demo.MyMod"                         // the class implementing SacredMod
    authors = listOf("MairwunNx (Pavel Erokhin)")     // or author("MairwunNx (Pavel Erokhin)"), one at a time
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    conflicts = listOf("other-mod")                   // or conflictsWith("other-mod")
    apiRange = "[1,2)"                                // API contracts this mod supports
    loaderRange = "[0.1.20,)"                         // optional loader release range
    apiVersion = "0.100.0"                              // added as compileOnly
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
```

Only `id` and `entrypoint` are required. `displayName` defaults to `id`, and
the descriptor version defaults to the project version. Other optional values
are omitted when blank or unset.

The generated descriptor uses Maven range notation. `api = "[1,2)"` means the
mod supports API contract 1 and stops before contract 2. The default is the
current contract and no other. Authors may widen or narrow the range, but it
must still contain the contract used by this toolchain.

`loaderRange` is optional. Set it only when the mod needs a particular Sacred
Mod Loader release. The plugin checks its syntax but cannot prove compatibility
with loader releases.

`apiVersion` is different from `apiRange`. It is the Maven artifact version of
`dev.ancaria.coderpack:api`, currently `0.100.0`, and the plugin adds that
dependency as `compileOnly`.

Build or install the mod with:

```
gradlew assembleSacredMod
gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
```

The first command writes
`build/sacred-mod/my-mod-1.0.0.jar` with runtime dependencies packed inside.
The second copies it to the configured `installTo` directory. You can hardcode
the path instead:
`installTo = file("D:/SteamLibrary/steamapps/common/Sacred Gold/mods")`.

| Task | What it does |
|---|---|
| `generateModDescriptor` | Writes `META-INF/declaration.toml` from the `sacred` block |
| `verifySacredMod` | Checks the packed jar and writes `build/reports/sacred-mod/verify.txt` |
| `assembleSacredMod` | Copies the verified fat jar to `build/sacred-mod` |
| `installSacredMod` | Copies that jar to `installTo` |

The plugin applies `java` and `com.gradleup.shadow`. A Kotlin or Groovy mod adds
its own language plugin, and a Kotlin one also adds `api-kotlin`, which is
packed like any other runtime dependency; it is inline extensions over the API,
so what reaches the jar is small. Java uses `options.release = 21`. Generated Kotlin
projects set `jvmTarget = JVM_21`, while Groovy projects set
`sourceCompatibility`, `targetCompatibility`, and `options.release` to 21.
Their standard libraries use `implementation`, so Shadow packs them.

Those runtimes affect jar size. From the `mod` template, the Java jar is about
1.8 KB, the Kotlin jar about 1.8 MB, and the Groovy jar about 7.8 MB. Each mod
has its own class loader, so the Kotlin standard library or Groovy runtime must
travel inside the mod jar.

### Verification runs before copying

`assembleSacredMod` depends on `verifySacredMod`. A jar that fails verification
never reaches `build/sacred-mod` or the game directory.

The linter reports errors for:

- an unreadable jar
- a missing `META-INF/declaration.toml`
- a descriptor without `id`, `entrypoint`, or `api`
- an invalid API range, one that excludes this toolchain's API contract, or an
  invalid loader range
- a missing, non-public, abstract, or incompatible entrypoint
- an entrypoint without a public no-argument constructor
- loader API classes packed into the mod jar
- a `@Subscribe` method with the wrong parameter count or type, or a return
  value
- signature files copied from a signed dependency

Warnings do not fail the build. They cover invalid ids, missing versions,
invalid or self-referential conflicts, packed zygote classes, class files newer
than Java 21, entrypoint inheritance that cannot be resolved from the jar, and
non-public listeners.

A failed build looks like this:

```
> Task :verifySacredMod FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':verifySacredMod' (registered by plugin 'dev.ancaria.coderpack').
> my-mod-1.0.0.jar: 1 error
    error   listener    demo.MyMod.onHero takes 2 parameters. The bus registers a listener with exactly one, and the parameter is what it subscribes to
```

The command-line entrypoint runs the same checks:

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

The linter reads bytecode with ASM and never loads mod classes. Loading them
could run a static initializer inside the build process.

### Design choices

**The descriptor is generated.** It repeats data the build already knows,
especially the version. Generating it prevents the jar and descriptor from
drifting apart.

**Runtime dependencies go into one jar.** A mod's class loader cannot see a
library unless that library is inside its jar. Shadow handles the packaging.

**The API is compile-only.** The loader already provides it. Packing a second
copy creates different classes with the same names and can cause a
`ClassCastException`.

**The plugin applies only `java`.** Other JVM language plugins build on it, so
the mod chooses and configures its own language.

## Repository layout

```
build/
  gradle/      the Gradle build
    plugin/    SacredPlugin, SacredExtension, Descriptor, Verify, and tests
  verify/      the linter, written in Java
  templates/   coderpack, project templates, languages, and DSLs
  maven/       design notes for a possible Maven plugin
```

`gradle/settings.gradle.kts` includes `verify/` and `templates/` as projects
outside the Gradle root directory, so `./gradlew build` covers all three.

Shared rules live in `verify/`. `Ids.valid` defines mod ids, and both the plugin
and scaffolder call it. `Verifier.API` defines the API contract.
`Verifier.API_RANGE` derives the default descriptor range from that contract,
and `Descriptor.API` reads it.

## Build and test

```
cd gradle
./gradlew build
```

The build needs a JDK on `PATH`. CI uses Temurin 21. There is no Java toolchain
block, so the JDK running Gradle performs the compilation. Java and Kotlin
outputs target Java 21. The wrapper pins Gradle 9.7.1 and checks its SHA-256
before use.

The plugin's group, version, and API artifact version live in
`gradle/gradle.properties`. CI reads the same `version` property when deciding
whether to publish.

The plugin tests use Gradle TestKit to build real projects. They check the fat
jar, generated descriptor, verification failure, id validation, API and loader
ranges, and configuration-cache compatibility.

The scaffolder's end-to-end test generates and builds all six language and DSL
pairs. It then requires an empty verifier report, including no warnings. The
remaining tests cover names, placeholders, output safety, template selection,
help, and resource discovery.

The verifier tests build real fixture jars. They cover each declaration,
content, entrypoint, listener, and range rule without loading classes.

The build enables:

```
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=fail
```

A Gradle `project` reference captured by a task action therefore fails here
instead of breaking a mod build later.

To exercise the standalone command line:

```
./gradlew :templates:installDist :verify:demoModJar
../templates/build/install/coderpack/bin/coderpack verify ../verify/build/demo/demo-mod.jar
```

`demoModJar` packs the passing fixture as a real mod jar. The scaffolder is the
same binary:

```
../templates/build/install/coderpack/bin/coderpack new demo-mod
cd demo-mod && ./gradlew assembleSacredMod
```

Useful development commands:

```
./gradlew test                  # all test tasks
./gradlew publishToMavenLocal   # what a mod on the same machine resolves
```

`./gradlew build` publishes the plugin and verifier to the local Maven
repository because the scaffolder's end-to-end test resolves them the same way
as a generated mod.

### Publishing

```
./gradlew publishToMavenLocal
```

That is enough for a mod on the same machine. For a release, CI reads `version`
from `gradle/gradle.properties` on pushes to `master`. If `v<version>` does not
exist, it publishes to two places: the linter and the scaffolder to Maven
Central as one signed bundle, the plugin to the Gradle Plugin Portal, which is
where `id("dev.ancaria.coderpack")` resolves from and the only place it does --
then creates that tag and attaches `coderpack-<version>.zip` to the GitHub
release.

Central takes `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY` and
`SIGNING_PASSWORD`; the portal takes `GRADLE_PUBLISH_KEY` and
`GRADLE_PUBLISH_SECRET`. Without a signing key the sign tasks are skipped rather
than failing. A Central upload waits in the portal for somebody to press
Publish, because an artifact there can never be deleted.

The implementation artifacts
`dev.ancaria.coderpack:plugin`, `dev.ancaria.coderpack:verify`, and
`dev.ancaria.coderpack:templates` share one version. Gradle also publishes the
`dev.ancaria.coderpack` plugin marker. The plugin's POM depends on the
verifier, while the IDE plugin consumes the templates library. Publishing only
part of that set would leave an unresolvable dependency.

## Possible additions

- a working Maven plugin under `maven/`
- templates for library mods or mods with configuration files
- an IDE run configuration that installs a mod and starts the game

## License

MIT. See [LICENSE](LICENSE).
