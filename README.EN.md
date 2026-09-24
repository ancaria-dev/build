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

Build tools for Sacred Gold mods: a Gradle plugin, a linter, and the
`coderpack` command that creates new projects.

You describe your mod in one `sacred { }` block. The plugin writes the mod
descriptor, packs your code and its libraries into one jar, and checks that
jar before it goes anywhere. A jar the loader would reject never reaches your
game folder.

This page covers the build. The mod API itself, with its events and
listeners, is documented in
[coderpack](https://github.com/ancaria-dev/coderpack).

## Getting started

You need a JDK. Gradle comes with the project.

1. Download `coderpack-<version>.zip` from the
   [releases](https://github.com/ancaria-dev/build/releases), unpack it, and
   add its `bin` folder to PATH.
2. Create a project and build it:

   ```
   coderpack new my-mod
   cd my-mod
   gradlew assembleSacredMod
   ```

3. Copy the jar from `build/sacred-mod/` into `<Sacred Gold>/mods`, or let
   Gradle do it:

   ```
   gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
   ```

Prefer an IDE? The
[IntelliJ IDEA plugin](https://github.com/ancaria-dev/idea) creates the same
project from File → New → Project.

## A new project

`coderpack new my-mod` treats `my-mod` as the mod id and checks it before it
writes anything. From the id it derives the display name `My Mod`, the package
`mods.mymod`, and the class `MyMod`. The project contains:

```
.gitignore
README.md
build.gradle.kts                     the filled-in sacred { } block
settings.gradle.kts                  mavenLocal first, then the plugin portal
registry.toml                        metadata for a mod repository
.github/workflows/build.yml          builds and releases each new version
dependencies.json                    the coderpack version CI downloads
gradlew, gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/mods/mymod/MyMod.java  onLoad and one working listener
```

The wrapper matches this repository's: Gradle 9.7.1 with the same SHA-256
checksum. Your first build takes one command, whatever Gradle is installed on
the machine.

| Option | What it changes |
|---|---|
| `--package dev.example.mymod` | The package and the entrypoint's folder |
| `--display-name "My Splendid Mod"` | The name players see |
| `--author "Your Name"` | The `authors` value. The default is the current account |
| `--description "One sentence"` | The line under the mod name |
| `--mod-version 0.2.0` | The mod version. The default is `1.0.0` |
| `--dir somewhere/else` | The output folder. The default is `./<name>` |
| `--template minimal` | The template: `mod` (the default) or `minimal` |
| `--language kotlin` | The mod language: `java` (the default), `kotlin`, or `groovy` |
| `--dsl groovy` | The build script language: `kotlin` (the default) or `groovy` |
| `--repo https://github.com/me/my-mod` | The project URL used for download links |
| `--no-registry` | Leaves out `registry.toml`, `dependencies.json`, and the workflow |
| `--git` | Runs `git init` and adds `--repo` as `origin` |
| `--force` | Writes into a folder that isn't empty |

Any language pairs with either build DSL. `coderpack templates` lists what's
available, and `--help` works on every command. A Kotlin project also gets
`dev.ancaria.coderpack:api-kotlin`, optional Kotlin extensions described in
[coderpack](https://github.com/ancaria-dev/coderpack).

`--git` runs after every file is written. If Git is missing, the project still
works, and the scaffolder never makes the first commit for you.

## The sacred block

`build.gradle.kts` describes the mod:

```kotlin
plugins {
    id("dev.ancaria.coderpack") version "0.200.0"
}

version = "1.0.0"

dependencies {
    // Anything the mod needs at run time. It travels inside the jar.
    implementation("org.jetbrains:annotations:26.0.2")
}

sacred {
    id = "my-mod"                                     // lowercase letters, digits, hyphens
    displayName = "My Mod"                            // what players read
    description = "One sentence, shown under the name in the mod list"
    version = "1.0.0"                                 // defaults to the project version
    entrypoint = "demo.MyMod"                         // the class extending SacredMod
    authors = listOf("MairwunNx (Pavel Erokhin)")     // or author("…"), one at a time
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    conflicts = listOf("other-mod")                   // or conflictsWith("other-mod")
    apiRange = "[3,4)"                                // API contracts the mod supports
    loaderRange = "[0.1.20,)"                         // optional loader release range
    apiVersion = "0.200.0"                            // added as compileOnly
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
```

Only `id` and `entrypoint` are required. `displayName` defaults to `id`, and
empty optional values stay out of the descriptor.

Three settings sound alike but mean different things:

- `apiRange` lists the API contracts the mod works with, in Maven range
  notation. `[3,4)` means contract 3, up to but not including 4. You may widen
  or narrow it, but it must include contract 3, the one this toolchain builds.
- `loaderRange` pins Sacred Mod Loader releases. Set it only when a mod needs a
  specific release. The plugin checks the syntax, not whether such a release
  exists.
- `apiVersion` is the Maven version of `dev.ancaria.coderpack:api`. The plugin
  adds it as `compileOnly`.

`installTo` can also be a fixed path:
`installTo = file("D:/SteamLibrary/steamapps/common/Sacred Gold/mods")`.

| Task | What it does |
|---|---|
| `generateModDescriptor` | Writes `META-INF/declaration.toml` from the `sacred` block |
| `verifySacredMod` | Checks the packed jar and writes `build/reports/sacred-mod/verify.txt` |
| `assembleSacredMod` | Copies the checked jar to `build/sacred-mod` |
| `installSacredMod` | Copies that jar to `installTo` |

Every library on `implementation` ends up inside the jar, and that includes a
language runtime. From the `mod` template, a Java jar weighs about 1.8 KB, a
Kotlin jar about 1.8 MB, and a Groovy jar about 7.8 MB. Every compiler targets
Java 21.

## What the linter checks

`assembleSacredMod` runs `verifySacredMod` first. An error stops the build.
The linter reports an error for:

- a jar it can't read, or one without `META-INF/declaration.toml`
- a descriptor without `id`, `entrypoint`, or `api`
- an unreadable API or loader range, or an API range without contract 3
- a missing or abstract entrypoint, or one that doesn't extend `SacredMod`
- an entrypoint that still implements `SacredMod` as the API 2 interface
- an entrypoint without a no-argument constructor
- loader API classes packed into the mod
- a `@Subscribe` method with anything but one event parameter, a return type
  other than `void` or that event's own `Mutation`, or a `MONITOR` method that
  returns a mutation
- signature files copied from a signed library

Warnings don't fail the build. They flag an invalid id, a missing version, an
invalid or self-referencing conflict, packed zygote classes, class files newer
than Java 21, an entrypoint whose parent class lives outside the jar, and a
non-public entrypoint, constructor, or listener.

A failed build looks like this:

```
> Task :verifySacredMod FAILED
...
> my-mod-1.0.0.jar: 1 error
    error   listener    demo.MyMod.onHero takes 2 parameters. The bus registers a listener with exactly one, and the parameter is what it subscribes to
```

The same checks run without Gradle. `coderpack verify` prints one report per
jar and exits with `1` on an error or `2` on bad arguments:

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

The linter reads bytecode with ASM and never loads your classes, so no static
initializer runs inside the build.

## Publishing a mod

Unless you pass `--no-registry`, every new project is also a mod repository
that the launcher can subscribe to:

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
gradlew assembleSacredMod
coderpack index
```

`registry.toml` is the part you maintain:

```toml
name = "My Mod"
description = "My Mod, a mod for Sacred Gold"
url = "https://github.com/me/my-mod"
releases = "https://github.com/me/my-mod/releases/download/{id}-v{version}/{file}"
```

`name`, `url`, and `releases` are required. `description` and `icon` are
optional. The `releases` template builds each download link and must contain
`{file}`. Without `--repo`, the scaffolder writes
`https://github.com/you/my-mod` and tells you which line to change.

`coderpack index` combines `registry.toml` with the descriptor of each built
jar and writes `sacred.mods.repository.json`. Don't edit that file by hand.
It has no timestamp, so `coderpack index --check` can compare it byte for
byte.

Commit and push. The included workflow builds the mod, regenerates the index,
commits it, and creates a `<id>-v<version>` release when that tag doesn't
exist yet. A push without a new version releases nothing. The workflow needs
no secret beyond GitHub's own token.

## Design choices

**The plugin writes the descriptor.** The build already knows the version and
the rest. A generated descriptor can't drift from the jar.

**Libraries go into one jar.** Each mod has its own class loader, which sees
only what's inside the jar. Shadow does the packing.

**The API is compile-only.** The loader already provides it. A second copy
creates different classes with the same names and can end in a
`ClassCastException`.

**The plugin applies only `java`.** Kotlin and Groovy plugins build on top of
it, so your mod picks and configures its own language.

## Repository layout

```
build/
  gradle/      the Gradle build
    plugin/    SacredPlugin, SacredExtension, Descriptor, Verify, and tests
  verify/      the linter, in Java
  templates/   coderpack, project templates, languages, and DSLs
  maven/       notes for a possible Maven plugin, not implemented yet
```

Shared rules live in `verify/`. `Ids.valid` defines a valid mod id, and both
the plugin and the scaffolder call it. `Verifier.API` holds the API contract,
and `Verifier.API_RANGE` derives the default range from it.

Templates, languages, and DSLs are resource folders under
`templates/src/main/resources/`, in `templates/<name>/`, `languages/<name>/`,
and `dsl/<name>/`. Each file lives where it varies: an entrypoint depends on
the template and language, a build script on the language and DSL, a settings
file on the DSL only. That gives 12 combinations from 27 files. The build
indexes the folders itself, so adding one takes no Kotlin code.

The scaffolder fills in `{{id}}`, `{{name}}`, `{{description}}`,
`{{version}}`, `{{package}}`, `{{packagePath}}`, `{{class}}`,
`{{entrypoint}}`, `{{author}}`, `{{repo}}`, `{{plugin}}`, and `{{api}}`, plus
every key except `description` from `language.properties` and
`dsl.properties`. An unresolved placeholder stops the run. Name a file
`_gitignore` to get `.gitignore`: resource copying skips dotfiles.

The same module is published as `dev.ancaria.coderpack:templates`, and the
IntelliJ IDEA plugin calls it instead of keeping its own copy of the
templates.

## Building

```
cd gradle
./gradlew build
```

The build needs a JDK on PATH. CI uses Temurin 21. There is no toolchain
block, so the JDK that runs Gradle compiles everything for Java 21. The
configuration cache is on and fails on any problem, so a task that captures
`project` breaks here and not in someone's mod.

The tests build real projects and real jars. The plugin tests use Gradle
TestKit. The scaffolder's end-to-end test generates and builds all six
language and DSL pairs and expects a report without a single warning. The
linter tests build a fixture jar for each rule and never load a class.
`./gradlew build` also publishes the plugin and linter to Maven Local, because
the end-to-end test resolves them the way a generated mod would.

To try the command line from source:

```
./gradlew :templates:installDist :verify:demoModJar
../templates/build/install/coderpack/bin/coderpack verify ../verify/build/demo/demo-mod.jar
../templates/build/install/coderpack/bin/coderpack new demo-mod
```

To test unreleased changes in a mod on the same machine, run
`./gradlew publishToMavenLocal` here and in `coderpack`. Generated projects
check Maven Local first.

## Releases

The version lives in `gradle/gradle.properties`. `pwsh tools/version.ps1
0.200.1` raises it together with `apiVersion` and every mention in the
READMEs. When you push a new version to `master`, CI publishes it in two
places:

- the linter and the scaffolder go to Maven Central as one signed bundle
- the plugin goes to the Gradle Plugin Portal, the only place
  `id("dev.ancaria.coderpack")` resolves from

Then CI creates the `v<version>` tag and attaches `coderpack-<version>.zip` to
the GitHub release. `plugin`, `verify`, and `templates` always share one
version: the plugin depends on the linter, and the IDE plugin on the
templates.

Central needs `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY`, and
`SIGNING_PASSWORD`. The portal needs `GRADLE_PUBLISH_KEY` and
`GRADLE_PUBLISH_SECRET`. An upload to Central waits until someone presses
Publish in the portal. The loader-wide release order is in
[CONTRIBUTING](https://github.com/ancaria-dev/.github/blob/master/CONTRIBUTING.EN.md).

## License

MIT, see [LICENSE](LICENSE).
