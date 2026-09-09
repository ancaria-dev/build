# Build

## Scope and authority

This repository provides build support for Sacred Gold mods. Its consumers are
mod authors, the `mods` repository, and the IntelliJ plugin in `idea`. The
launcher, host, and injected agent do not read this repository at run time.

Use this order when documentation and code disagree:

1. The current implementation and tests
2. `README.md`, `README.EN.md`, and `README.DE.md`
3. This guide
4. `maven/README.md`, which is a design placeholder and has no implementation

Keep this guide synchronized with all three user-facing READMEs after behavior,
commands, versions, or generated output changes.

## Module boundaries

`gradle/` is a standalone Gradle build named `coderpack-build`. Its
`settings.gradle.kts` includes:

- `:plugin` from `gradle/plugin`
- `:verify` from `../verify`
- `:templates` from `../templates`

Build from `gradle/`. The external project directories intentionally have no
wrapper of their own.

`gradle/plugin/` implements the Gradle plugin with id
`dev.ancaria.coderpack`. The main classes are `SacredPlugin`,
`SacredExtension`, `Descriptor`, and `Verify`.

`verify/` is a plain Java library, not a build system. `Verifier` is its public
entry point. `Report`, `Finding`, `Level`, `Ids`, and `Ranges` are also public.
`Verifier.verify(Path)`, `describe(Path)`, `parse(String)`, and `list(String)`
serve both the Gradle plugin and `coderpack`. `Declaration`, `Contents`,
`Entrypoint`, and `Listeners` are the four checks. `Jar`, `Klass`, `Toml`, and
`Mod` read jars and descriptors. There is no verifier `Cli` class.

`templates/` is a Kotlin application and a published library. It builds the
`coderpack` command line. `installDist` writes `bin/coderpack` and
`bin/coderpack.bat`. The `idea` project consumes
`dev.ancaria.coderpack:templates` and calls `Scaffold` instead of carrying a
second template copy.

`templates/src/main/kotlin/dev/ancaria/coderpack/templates/` contains:

- `Main`, which dispatches commands
- `New`, `Verify`, and `Registry`, which implement `new`, `verify`, and `index`
- `Args`, `Names`, `Index`, `Templates`, `Languages`, `Dsls`, `Scaffold`,
  `Wrapper`, `Versions`, `Json`, `Git`, `Fail`, and `Help`

`maven/` contains only `README.md`. There is no Maven plugin, goal, artifact,
test, or release output. The note proposes descriptor generation during
`process-resources`, shaded packaging with the API scoped as `provided`, and an
optional install goal. Treat those as design requirements for a future Maven
implementation, not current commands. A real implementation must also preserve
the same descriptor, verification-before-copy, API exclusion, Java 21, and
runtime-dependency invariants as the Gradle path.

## Current versions

The authoritative build properties are in `gradle/gradle.properties`:

- group `dev.ancaria.coderpack`
- plugin, verifier, templates, and command-line version `0.99.0`
- API artifact version `0.99.0`
- API contract `1`, declared as `Verifier.API`
- default API range `[1,2)`, derived as `Verifier.API_RANGE`

The wrapper uses Gradle `9.7.1` and verifies SHA-256
`acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a`.
CI uses Temurin 21.

`gradle/gradle/libs.versions.toml` pins Shadow `9.6.1`, ASM `9.8`, and JUnit
`5.11.4`. Shadow appears both as a plugin reference and as a library because
the plugin applies it at run time.

Generated Kotlin mods use Kotlin `2.4.10`. Generated Groovy mods use Groovy
`5.1.1`. Those compiler versions belong in each language's
`language.properties`, not in Gradle build logic. The functional plugin test
resolves `org.jetbrains:annotations:26.0.2`.

## Gradle plugin behavior

`SacredPlugin.apply` applies `java` and applies `com.gradleup.shadow` by id. It
registers four tasks in the `sacred` group:

| Task | Behavior |
|---|---|
| `generateModDescriptor` | Writes `META-INF/declaration.toml` below `build/generated/sacred-mod`. `processResources.from(descriptor)` carries the task dependency. |
| `verifySacredMod` | Verifies `shadowJar` and writes `build/reports/sacred-mod/verify.txt`. An error becomes a `GradleException` containing the full report. |
| `assembleSacredMod` | Depends on verification and copies the fat jar to `build/sacred-mod`. |
| `installSacredMod` | Copies the assembled jar to `sacred.installTo`. If unset, the destination remains `build/sacred-mod`. |

`check` also depends on `verifySacredMod`. Keep verification on the packaging
path. A jar that the loader would reject must not reach either
`build/sacred-mod` or the game directory.

The plugin configures `shadowJar` as follows:

- empty `archiveClassifier`
- `archiveBaseName` from `sacred.id`, falling back to the project name
- `duplicatesStrategy = INCLUDE`
- exclude `META-INF/*.SF`, `META-INF/*.DSA`, and `META-INF/*.RSA`
- exclude `module-info.class` and
  `META-INF/versions/*/module-info.class`

`INCLUDE` is required before Shadow transformers can merge service
registrations and repeated `.kotlin_module` entries. Do not replace it with the
default duplicate handling.

The `sacred` extension has these properties:

- `id`
- `displayName`, defaulting to `id`
- `description`
- `version`, defaulting to the project version
- `entrypoint`
- `authors`, with `author(name)`
- `website`
- `repository`
- `conflicts`, with `conflictsWith(id)`
- `apiRange`, defaulting to `[1,2)`
- `loaderRange`
- `apiVersion`
- `installTo`

Only `id` and `entrypoint` are required. `id` must match
`[a-z0-9]([a-z0-9-]*[a-z0-9])?`. `Descriptor.generate` also rejects an invalid
or self-referential conflict, a blank entrypoint, an unreadable API range, an
API range that excludes `Verifier.API`, and an unreadable loader range.

`apiRange`, `loaderRange`, and `apiVersion` are different values:

```toml
api = "[1,2)"
loader = "[0.1.20,)"
```

`api` names compatible API contracts. Authors may widen or narrow it, but the
range must contain contract `1`, which is the contract this toolchain builds.
Both `Descriptor.generate` and the packed-jar verifier enforce that rule.

`loader` names compatible Sacred Mod Loader releases. It is omitted when
`loaderRange` is unset or blank. The plugin and verifier check only its syntax
because this repository cannot establish which launcher releases exist.

`apiVersion` names the Maven artifact
`dev.ancaria.coderpack:api:<apiVersion>`. The plugin adds it as `compileOnly`
only when the property has a value. Generated projects set it to `0.99.0`. The
loader already provides the API. Packing another copy can produce
`ClassCastException` between classes with identical names.

`Descriptor.API` reads `Verifier.API_RANGE`. Do not replace it with a second
literal. Raising the API contract requires synchronized changes to:

- `Verifier.API` in this repository
- `Api.VERSION` in `coderpack`
- `mods.API` in `launcher`

The `coderpack` `api-contract` job checks those three values. Range parsing is
also implemented in three places: `verify.Ranges`, the zygote's `Ranges`, and
the launcher's `pin`. Keep their test corpora aligned. A case added to one
belongs in all three.

The plugin applies only `java`. Kotlin and Groovy projects apply their own
language plugins. Every `JavaCompile` uses `options.release = 21`. Do not add a
Java toolchain. The installed JDK runs Gradle, and the explicit compiler
targets control the emitted bytecode without downloading another JDK.

## Scaffolder behavior

`coderpack new <name>` validates `<name>` with `Ids.valid` before writing.
`Names` derives `My Mod`, `mods.mymod`, and `MyMod` from `my-mod`.

Supported options are:

- `--template <name>`
- `--language <name>`
- `--dsl <name>`
- `--dir <path>`
- `--package <package>`
- `--display-name <text>`
- `--description <text>`
- `--author <name>`
- `--mod-version <version>`
- `--repo <url>`
- `--no-registry`
- `--git`
- `--force`

`--help` and `-h` work on every subcommand and exit `0`. `coderpack help`
prints the complete command list and reuses `New.HELP` for the `new` options.
Use `coderpack templates` to list the available templates, languages, and
build DSLs from the generated resource index.

`Scaffold.plan` renders every path and file into memory. `Scaffold.write` then
checks the target and writes it. Without `--force`, an existing non-empty
directory is rejected before the first output file. Unknown templates,
languages, DSLs, options, and unresolved placeholders also fail before output.

A default project contains:

- `.gitignore`
- a template README
- `build.gradle.kts`
- `settings.gradle.kts`
- `registry.toml`
- `dependencies.json`
- `.github/workflows/build.yml`
- `gradlew` and `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- a Java entrypoint under `src/main/java`

Java is the default language. Kotlin DSL is the default build syntax. The
current templates are `mod` and `minimal`. The languages are `java`, `kotlin`,
and `groovy`. The build DSLs are `kotlin` and `groovy`. Any language can pair
with either DSL.

Generated settings put `mavenLocal()` first. Plugin management then uses
`gradlePluginPortal()`. Dependency resolution then uses `mavenCentral()`. For
unreleased local work, run `publishToMavenLocal` in both this repository and
`coderpack`. The former publishes the plugin, verifier, and templates. The
latter publishes the API.

`wrapperResources` packages this build's `gradlew`, `gradlew.bat`, and
`gradle/wrapper/` files. Do not check in another wrapper below the template
tree. Generated projects must use the same Gradle `9.7.1` distribution and
checksum as this build.

`--git` runs after all files are written. It runs `git init` and adds the
normalized `--repo` URL as `origin`. Git failure does not remove or invalidate
the generated project. The scaffolder does not create a commit.

## Template layout

Five resource layers produce a project:

1. `templates/src/main/resources/common/files/`
2. `templates/src/main/resources/repository/files/` when SRML is enabled
3. `templates/src/main/resources/dsl/<dsl>/files/`
4. `templates/src/main/resources/languages/<language>/files/` and
   `languages/<language>/dsl/<dsl>/`
5. `templates/src/main/resources/templates/<template>/files/` and
   `templates/<template>/lang/<language>/`

Later, more specific layers can shadow earlier paths.

`common/files/` currently contributes `_gitignore`. `repository/files/`
contributes `registry.toml`, `dependencies.json`, and `_github/workflows/build.yml`.

Each template has `template.properties`, shared `files/`, and one
`lang/<language>/` tree per supported entrypoint. Each language has
`language.properties`, optional shared `files/`, and one build script per DSL
under `dsl/<dsl>/`. Each DSL has `dsl.properties` and a settings file below
`files/`.

Entrypoints vary by template and language. Build scripts vary by language and
DSL. Settings files vary only by DSL. The README, `.gitignore`, and SRML files
are shared. The current source contains 27 indexed resource files after
dot-segment scratch files are excluded. It generates 12 template, language,
and DSL combinations. A third template requires one properties file, one
README, and three entrypoints. It does not require new build scripts, settings
files, or SRML files.

`resourceIndex` walks `src/main/resources` and writes
`dev/ancaria/coderpack/templates/index.txt` beside the classes. `Index` reads
that list once. `Templates`, `Languages`, and `Dsls` filter it by prefix.
Paths containing a dot-prefixed segment are excluded. This prevents local
`.gradle` files from entering every generated project.

The fixed placeholders are:

- `{{id}}`
- `{{name}}`
- `{{description}}`
- `{{version}}`
- `{{package}}`
- `{{packagePath}}`
- `{{class}}`
- `{{entrypoint}}`
- `{{author}}`
- `{{repo}}`
- `{{plugin}}`
- `{{api}}`

Every property key except `description` from `language.properties` and
`dsl.properties` also becomes a placeholder. Current extra keys are `srcDir`,
`srcExt`, `kotlin`, `groovy`, `buildFile`, and `settingsFile`. Merge order is
DSL values, then language values, then caller values. Caller values win.

`Scaffold.PLACEHOLDER` must not match GitHub Actions expressions such as
`${{ github.token }}`. Keep the negative lookbehind and its test.

Any path segment beginning with `_` becomes dot-prefixed through
`Scaffold.dotted`. Never add a dot-prefixed template path. Gradle copy defaults
and `resourceIndex` would omit it.

`versionResource` writes `tool`, `plugin`, and `api` to
`coderpack.properties`. Templates must use `{{plugin}}` and `{{api}}` instead
of hardcoded first-party versions.

### Generated language builds

Java applies only `dev.ancaria.coderpack`. The plugin sets
`JavaCompile.options.release = 21`.

Kotlin applies `kotlin("jvm")`, uses
`implementation(kotlin("stdlib"))`, and sets `jvmTarget` to `JVM_21`.

Groovy applies `groovy`, uses
`implementation("org.apache.groovy:groovy:5.1.1")`, and configures
`GroovyCompile` with `sourceCompatibility = "21"`,
`targetCompatibility = "21"`, `options.release = 21`, and UTF-8 encoding.

Keep runtime language libraries on `implementation`. Shadow packs that
configuration. Moving one to `compileOnly` produces a project that compiles
and then fails in the game with `NoClassDefFoundError`.

Keep a target for every compiler. A Java 25 class has class-file version `69`.
The verifier warns above version `65`, which is Java 21. Approximate `mod`
template sizes are 1.8 KB for Java, 1.8 MB for Kotlin, and 7.8 MB for Groovy.
The larger jars contain the required language runtime.

## SRML repository generation

Unless `--no-registry` is passed, every generated project is also an SRML mod
repository:

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
./gradlew assembleSacredMod
coderpack index
```

`--no-registry` omits exactly `registry.toml` and
`.github/workflows/build.yml`. It does not change the rest of the project.

`coderpack new` writes `registry.toml`. It never writes
`sacred.mods.repository.json` because no jar exists yet. `registry.toml`
currently has four data fields: `name`, `description`, `url`, and `releases`.
`name`, `url`, and `releases` are required by `Registry`. `description` and
`icon` are optional inputs.

`--repo` fills `url` and the `releases` template. Without it, the generated URL
is `https://github.com/you/<id>`, and the command report tells the author to
replace it.

`coderpack index [<repository>]` reads `registry.toml` through
`Verifier.parse`. By default it finds both
`<root>/build/sacred-mod/*.jar` and
`<root>/*/build/sacred-mod/*.jar`. `--jars <dir>` overrides jar discovery.
`--out <file>` overrides the output path.

The index copies `id`, `name`, `version`, `description`, `api`, optional
`loader`, `authors`, `website`, and `conflicts` from each jar descriptor.
`source`, `icon`, `file`, `size`, `sha256`, and the rendered download URL come
from repository layout or jar bytes. Duplicate mod ids fail the run.

A root `icon.png` belongs to the only mod when the repository contains one
jar. With multiple mods, a mod icon belongs at `<id>/icon.png`, and the root
icon describes the repository.

Index generation rejects jars with verifier errors. Warnings are retained in
the report but do not make `Report.ok()` false, so they do not block indexing.
Do not weaken an error to make publishing pass.

`--check` writes nothing and returns `1` when the current
`sacred.mods.repository.json` differs from generated output. The JSON has no
timestamp so comparison remains deterministic. Do not edit it by hand. Fix the
jar or `registry.toml`, run `coderpack index`, and commit the result.

```
coderpack index --check
```

The generated workflow builds the mod, downloads `coderpack-*.zip` from the
`ancaria-dev/build` release pinned in the generated `dependencies.json` (never
"latest": a bad `build` release should not be able to break every SRML
repository's CI at once), runs `coderpack index --check`, and creates one
release per new `<id>-v<version>` tag on the repository's default branch. It
supports both single-mod and multi-mod layouts. `${{ github.token }}` is
enough. No separate secret is required. This workflow cannot work until the
first `build` release exists. `dependencies.json` is written with `Versions.plugin`,
so a generated project starts pinned to the toolchain that generated it;
raising the pin later is a normal edit.

## Verifier contract

`Verifier.verify(Path)` opens a jar once and runs four checks. `ERROR` makes
`Report.ok()` false. `WARNING` is printed but does not fail verification,
packaging, command-line exit status, or index generation.

| Check | Errors | Warnings |
|---|---|---|
| `Declaration` | Unreadable jar, missing `META-INF/declaration.toml`, missing `id`, `entrypoint`, or `api`, unreadable ranges, API range excluding contract `1` | Invalid id, missing version, invalid conflict id, self-conflict |
| `Contents` | Loader API classes inside the jar, top-level signature files under `META-INF` | Zygote classes, class files above version 65 |
| `Entrypoint` | Missing, non-public, abstract, incompatible entrypoint, or missing public no-argument constructor | Unreadable class version, external superclass that prevents proof of `SacredMod` implementation |
| `Listeners` | `@Subscribe` with a parameter count other than one, a non-event parameter, or a non-void return | Non-public listener |

The exact id pattern is `[a-z0-9]([a-z0-9-]*[a-z0-9])?`.

An event type is an object in `dev/ancaria/coderpack/api/event/` other than
`Guard`, or a class inside the jar whose superclass chain reaches that package.

Never load mod classes in the verifier. `Klass.read` uses ASM
`ClassReader.SKIP_CODE` and keeps only the metadata needed by the checks.
`Class.forName` could execute a static initializer in an untrusted jar.

`verify` depends on ASM and no other external library. The Gradle plugin carries
it transitively into every mod build. Adding a verifier dependency adds it to
every plugin consumer.

A new verifier error is allowed only when the loader or launcher would reject
the same jar. Read the runtime implementation first and make the finding explain
that behavior. Anything less is a warning.

`coderpack verify <jar> [<jar> ...]` prints one report per jar. It returns `1`
when any report has an error and `2` for invalid arguments. Warnings do not
change the return code.

Install a verified mod by configuring `sacred.installTo` and running:

```
./gradlew installSacredMod -PsacredDir="<Sacred Gold>"
```

## Build and test

The build requires a JDK on `PATH` and a network connection on a cold cache.
There is no Java toolchain. Java and Kotlin compilation target Java 21. The
Groovy templates also set all relevant targets to 21.

```
cd gradle
./gradlew build
```

The slow tests are `:plugin:test` and `:templates:test` because they start
nested Gradle builds. `:verify:test` builds fixture jars directly.

Run the standalone command-line path with:

```
./gradlew :templates:installDist :verify:demoModJar
../templates/build/install/coderpack/bin/coderpack verify ../verify/build/demo/demo-mod.jar
```

`demoModJar` is the clean verifier fixture. The same distribution runs the
scaffolder:

```
../templates/build/install/coderpack/bin/coderpack new demo-mod
cd demo-mod
./gradlew assembleSacredMod
```

Useful commands:

```
./gradlew test
./gradlew publishToMavenLocal
```

The unqualified `test` command runs test tasks across the included projects.
`publishToMavenLocal` publishes the local artifacts used by generated mods and
the `idea` plugin.

`gradle/gradle.properties` enables:

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=fail
```

Keep tasks compatible with the configuration cache. Do not capture a `Project`
reference in a task action.

### Plugin tests

`SacredPluginTest` currently has seven TestKit cases. They cover fat-jar
packaging, the generated `[1,2)` API range, verification before copy, id
validation, custom API and loader ranges, omitted loader ranges, API ranges
that exclude the current contract, and invalid range syntax.

The packaging case builds
`build/sacred-mod/demo-mod-1.2.3.jar`. It asserts the descriptor, the mod class,
`org/jetbrains/annotations/NotNull.class`, and the absence of
`dev/ancaria/coderpack/api/SacredMod.class`. The loader API fixture is added
through a `loaderApi` source set with `compileOnly`.

`tasks.test` depends on `pluginUnderTestMetadata` so nested Gradle receives the
plugin and its dependencies.

### Template tests

`EndToEndTest` generates and builds all six language and DSL pairs. It appends
a `flatDir` API repository, resolves the plugin and verifier from Maven Local,
runs `assembleSacredMod --configuration-cache`, verifies an empty findings
list, generates the SRML index, and checks id, version, file name, and SHA-256.

`:templates` creates `apiStub` from
`../verify/src/fixtures/java/dev/ancaria/coderpack/api/**` and packages
`api-<apiVersion>.jar`. Keep one API stub source instead of introducing a
second copy.

`NamesTest`, `ScaffoldTest`, and `NewTest` cover naming, all 12 combinations,
placeholder precedence, unresolved placeholders, `${{ ... }}` preservation,
target-directory safety, options, help, registry selection, resource discovery,
and exact generated file sets.

### Verifier tests

`VerifierTest` currently has 22 cases. `RangesTest` has 6. They build real jars
from `verify/src/fixtures/java`, isolate one rule where applicable, and assert
the exact findings count. Do not replace them with mocked jars.

## Publishing and release

The release version lives only in `gradle/gradle.properties`. Raising that
`version` is what makes CI eligible to publish. `tools/version.ps1` prints it
with no argument, or raises both `version` and `apiVersion` together --
plus the matching mentions in `Descriptor.kt`'s Javadoc and the three
READMEs, with `pwsh tools/version.ps1 0.99.1`. Edit `gradle.properties` by
hand instead when the two numbers need to move apart.

Local publication:

```
cd gradle
./gradlew publishToMavenLocal
```

Publishing needs credentials for two different places, and neither is needed to
build. Maven Central takes `CENTRAL_USERNAME` and `CENTRAL_PASSWORD` (a Sonatype
user token, not an account password) plus `SIGNING_KEY` and `SIGNING_PASSWORD`;
the Gradle Plugin Portal takes `GRADLE_PUBLISH_KEY` and
`GRADLE_PUBLISH_SECRET`. Without a signing key the sign tasks are skipped rather
than failing.

`.github/workflows/build.yml` uses `gradle/` as its working directory. It runs
`./gradlew build --no-daemon`, installs the command-line distribution, and
verifies `demo-mod.jar` on pushes and pull requests.

On `master`, CI publishes only when remote tag `v<version>` does not exist, and
it publishes to two places because the two halves of this repository are
consumed differently. `nmcpPublishAggregationToCentralPortal` uploads the linter
and the scaffolder to Maven Central as one signed bundle; `:plugin:publishPlugins`
uploads the plugin to the Gradle Plugin Portal, which is where
`id("dev.ancaria.coderpack")` in a mod build script resolves from and the only
place it does. Then it creates the tag through the GitHub release and attaches
`templates/build/distributions/coderpack-*.zip`. A push that leaves `version`
unchanged builds but does not publish.

`nmcpZipAggregation` builds the Central bundle locally without uploading it,
which is how to check what a release would contain. `publishingType` in
`gradle/build.gradle.kts` is `USER_MANAGED`, so an upload waits in the portal
for somebody to press Publish. A Central artifact can never be deleted. The
plugin is not in that bundle on purpose: the portal hosts it, and the portal
proxies Central for the `verify` dependency its POM names.

Publish `plugin`, `verify`, and `templates` together at the same version. The
plugin POM depends on `verify`. The `idea` plugin depends on `templates`.
Publishing only part of the set leaves consumers with unresolved dependencies.

The templates sources jar must depend on `resourceIndex`, `versionResource`,
and `wrapperResources`. Gradle otherwise rejects the build because generated
resources could race the archive task.

`./gradlew build` publishes `:plugin` and `:verify` to Maven Local as test
prerequisites because `:templates:test` resolves them like a generated mod.

## Change rules

- Do not add a Java toolchain.
- Do not apply Kotlin or Groovy from `SacredPlugin`.
- Do not apply Shadow by class. Apply id `com.gradleup.shadow`.
- Do not remove `duplicatesStrategy = INCLUDE`.
- Do not remove verification from `assembleSacredMod`, `installSacredMod`, or
  `check`.
- Do not remove the `Verifier.builds` check from descriptor generation.
- Do not load classes in `verify`. Use ASM.
- Keep `verify` free of external dependencies other than ASM unless the
  transitive cost to every plugin consumer is justified.
- Add an extension property to `Descriptor` as an `@Input` or optional
  `@Input`, then wire it in `SacredPlugin`. `Descriptor` is cacheable and
  silently ignores unwired extension state.
- Keep shared id, API contract, range, and descriptor parsing rules in
  `verify`. The plugin and scaffolder must call those definitions.
- Add a template under
  `templates/src/main/resources/templates/<name>/`.
- Add a language under
  `templates/src/main/resources/languages/<name>/`.
- Add a DSL under `templates/src/main/resources/dsl/<name>/`, then add one
  matching build script for every language under
  `languages/<language>/dsl/<dsl>/`.
- Put a build script under the language, not the template.
- Put a file that varies only by DSL under the DSL.
- Put a file that varies by language and DSL under the language's `dsl/` tree.
- Put a file shared by all templates under `common/files/`.
- If a template needs to override an earlier layer, shadow the same path in the
  template. Do not invent a fourth variation tree.
- Keep `buildFile` and `settingsFile` in every `dsl.properties`.
- Do not reorder `Scaffold.plan` parameters. `dsl` and `repository` follow
  `values` and have defaults for existing callers, including `idea`.
- Do not hardcode first-party plugin or API artifact versions in templates.
- Keep third-party compiler versions in `language.properties`.
- Do not add dot-prefixed resource paths. Use `_` and let `Scaffold.dotted`
  restore the dot.
- Preserve GitHub Actions `${{ ... }}` expressions through placeholder
  rendering.
- Keep runtime language dependencies on `implementation`.
- Set the bytecode target independently for Java, Kotlin, and Groovy.
- Do not make `coderpack new` generate
  `sacred.mods.repository.json`.
- Do not hand-edit `sacred.mods.repository.json`.
- Classify a new verifier condition as `ERROR` only when runtime code rejects
  the same jar. Otherwise use `WARNING`.

## Relations to other repositories

This repository reads no sibling checkout and builds alone.
`dev.ancaria.coderpack:api` is published by `coderpack`.

`launcher/tools/build.ps1` stages jars matching
`<workspace>/mods/*/build/sacred-mod/*.jar`.

`idea` is the only repository that reads this repository's Kotlin API. It calls
`Scaffold.plan` and `Scaffold.write`, and reads `Templates.names`,
`Languages.names`, `Dsls.names`, and each `describe` method for its New Project
dialog. Its `settings.gradle.kts` includes `../build/gradle` as a composite
build when the repositories are adjacent. This repository does not depend on
`idea`.

## Known costs and failure modes

- Plugin tests need Maven Central for
  `org.jetbrains:annotations:26.0.2`.
- Template end-to-end tests need the Gradle Plugin Portal for Kotlin and Maven
  Central for Groovy.
- A cold `:templates:test` can take several minutes.
- Kotlin and Groovy mod jars are large because each mod has an isolated class
  loader and must carry its language runtime.
- A generated project's release workflow depends on the latest published
  `coderpack` zip. It fails before this repository has its first release.
- Running `./gradlew` from `verify/` or `templates/` fails because the wrapper
  belongs to `gradle/`.
- `:templates` uses `embedded-kotlin`. Keep
  `implementation(embeddedKotlin("stdlib"))` because the application
  distribution needs the standard library at run time. Without it, startup
  fails with `NoClassDefFoundError: kotlin/jvm/internal/Intrinsics`.
