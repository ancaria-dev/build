# build

Workspace rules, release chain, and the Central-upload rule: see `../CLAUDE.md`.

## Scope

- This repository holds the Gradle plugin `dev.ancaria.coderpack` (`gradle/plugin`), the mod linter (`verify/`), and the `coderpack` command line and scaffolder (`templates/`). It reads no sibling checkout.
- Consumers: mod authors, `mods`, and `idea`. The loader never reads this repository at run time.
- When docs and code disagree, trust in order: code and tests, the three READMEs, this guide, `maven/README.md`.
- Keep this guide and all three READMEs in sync after changing behavior, commands, or generated output.
- `maven/` is a design note only: no plugin, goal, artifact, or test. A future Maven implementation must keep the Gradle path's invariants: same descriptor, verify before copy, API excluded, Java 21, runtime dependencies packed.

## Building

- Run Gradle only from `gradle/`. `verify/` and `templates/` are included builds with no wrapper of their own.
- `./gradlew build` publishes `:plugin` and `:verify` to Maven Local, because `:templates:test` resolves them like a generated mod.
- `./gradlew publishToMavenLocal` publishes plugin, verifier, and templates for local generated mods and `idea`. For unreleased work, also run it in `coderpack` for the API.
- `:plugin:test` and `:templates:test` start nested Gradle builds and are slow; a cold `:templates:test` takes minutes. Plugin tests need Central; template tests need the Plugin Portal (Kotlin) and Central (Groovy).
- Never add a Java toolchain. Every `JavaCompile` uses `options.release = 21`, so no JDK is downloaded.
- Keep tasks configuration-cache compatible (`problems=fail`). Never capture a `Project` in a task action.
- Keep `implementation(embeddedKotlin("stdlib"))` in `:templates`; without it the distribution fails with `NoClassDefFoundError: kotlin/jvm/internal/Intrinsics`.
- The templates sources jar must depend on `resourceIndex`, `versionResource`, and `wrapperResources`, or Gradle rejects the race.
- Demo: `./gradlew :templates:installDist :verify:demoModJar`, then `../templates/build/install/coderpack/bin/coderpack verify ../verify/build/demo/demo-mod.jar`.

## Gradle plugin

- Apply only `java`, and apply Shadow by id `com.gradleup.shadow`, never by class. Never apply Kotlin or Groovy; projects apply their own language plugin.
- Keep verification on the packaging path: `assembleSacredMod`, `installSacredMod`, and `check` depend on `verifySacredMod`. A jar the loader would reject must never reach `build/sacred-mod` or the game.
- Keep `duplicatesStrategy = INCLUDE`. Shadow transformers need it to merge service files and `.kotlin_module` entries.
- Keep the `plain` classifier on the thin `jar`. The mod id usually equals the project name, and both jars would otherwise write the same path.
- Add an extension property to `Descriptor` as an `@Input` or optional `@Input`, then wire it in `SacredPlugin`. `Descriptor` is cacheable and silently ignores unwired state.
- `apiRange` must include `Verifier.API`; `Descriptor.generate` and the verifier both enforce it. Keep the `Verifier.builds` check.
- `loaderRange` is checked for syntax only; this repository cannot know which launcher releases exist.
- `apiVersion` adds `dev.ancaria.coderpack:api` as `compileOnly`. The loader provides the API; a packed copy causes `ClassCastException`.
- `Descriptor.API` reads `Verifier.API_RANGE`. Never add a second literal.
- Keep id, API contract, range, and descriptor parsing in `verify`. The plugin and scaffolder call it.
- `Verifier.API` is one of three API contract constants and `verify.Ranges` one of three range parsers. See `../coderpack/CLAUDE.md`.

## Scaffolder

- `Scaffold.plan` renders everything in memory; `Scaffold.write` then checks the target. Unknown options, templates, languages, DSLs, unresolved placeholders, and a non-empty target without `--force` fail before the first file.
- Never reorder `Scaffold.plan` parameters. `dsl` and `repository` follow `values` with defaults, for existing callers including `idea`.
- `--git` runs `git init` and adds `origin` after writing. A git failure keeps the project. Never create a commit.
- `idea` calls `Scaffold.plan`, `Scaffold.write`, and the `names`/`describe` methods of `Templates`, `Languages`, `Dsls`. Keep them stable. Never create a second copy of the templates.

## Templates

- Layers, later shadowing earlier paths: `common/files/`; `repository/files/` when SRML is on; `dsl/<dsl>/files/`; `languages/<language>/files/` and `languages/<language>/dsl/<dsl>/`; `templates/<template>/files/` and `templates/<template>/lang/<language>/`. All under `templates/src/main/resources/`.
- Put a build script under the language's `dsl/<dsl>/`, never under the template. Put a file that varies only by DSL under the DSL. Put a file shared by all templates under `common/files/`.
- A new template needs `template.properties`, a README, and one entrypoint per language. A new language needs `language.properties` and one build script per DSL. A new DSL needs `dsl.properties` (with `buildFile` and `settingsFile`) and a build script in every language.
- To override an earlier layer, shadow the same path in the template. Never invent another variation tree.
- `language.properties` and `dsl.properties` keys (except `description`) become placeholders. Merge order: DSL, language, caller; the caller wins.
- Keep third-party compiler versions in `language.properties`. Use `{{plugin}}` and `{{api}}` for first-party versions; never hardcode them.
- `Scaffold.PLACEHOLDER` must never match GitHub Actions `${{ ... }}`. Keep its negative lookbehind and test.
- Never add a dot-prefixed resource path; Gradle copy and `resourceIndex` skip it. Name it with `_` and let `Scaffold.dotted` restore the dot.
- Generated projects use this build's wrapper through `wrapperResources`. Never check in another wrapper under the template tree.
- Keep language runtime libraries on `implementation`, which Shadow packs. `compileOnly` compiles and then fails in the game with `NoClassDefFoundError`.
- Kotlin templates declare `dev.ancaria.coderpack:api-kotlin` as `implementation`, because the loader does not provide it.
- Keep a library the loader does not provide out of `dev/ancaria/coderpack/api/`. `Contents` refuses that package in a mod jar; `api-kotlin` lives in `dev.ancaria.coderpack.ktx` for this reason.
- Set the bytecode target for every compiler separately: Java via the plugin, Kotlin `jvmTarget = JVM_21`, Groovy source, target, and release 21.

## SRML index

- `--no-registry` omits exactly `registry.toml`, `dependencies.json`, and `.github/workflows/build.yml`.
- `coderpack new` never writes `sacred.mods.repository.json`; no jar exists yet.
- Never hand-edit `sacred.mods.repository.json`. Fix the jar or `registry.toml` and run `coderpack index`.
- Never add a timestamp to the index; `--check` must be deterministic.
- `coderpack index` refuses jars with linter errors; warnings do not block. Never weaken an error to make publishing pass.
- With one jar, a root `icon.png` is the mod's. With several, a mod icon is `<id>/icon.png` and the root icon is the repository's.
- The generated workflow does not run `--check`: jars that pack a language runtime differ by a few bytes between machines.
- The generated workflow downloads `coderpack-*.zip` from the `build` release pinned in its `dependencies.json`, written with `Versions.plugin`. It fails until that release exists.
- It indexes a mod whose `<id>-v<version>` tag exists from that release's asset, and a new version from the fresh jar, because the launcher checks the SHA-256 the index published.
- It regenerates the index before the release step and commits with the workflow token, which starts no new run. On pull requests it generates without comparing or committing.

## Linter

- Never load mod classes. `Klass.read` uses ASM with `SKIP_CODE`; `Class.forName` could run a static initializer from an untrusted jar.
- Keep ASM the only external dependency of `verify`. The plugin carries it into every mod build.
- Add an `ERROR` only when the loader or launcher rejects the same jar; read that runtime code first and explain it in the finding. Anything else is a `WARNING`.
- An event type is a top-level type in `dev/ancaria/coderpack/api/event/` other than `EventMutation`, `Decides`, `Fold`, `Delivery`, or a jar class whose superclass chain reaches that package. Nested types are never events. `idea`'s marker draws the same line.
- A listener returns `void`, or exactly `<its event>$Mutation`. Another event's mutation compiles and would be folded into the wrong decision. A `MONITOR` listener returning a mutation is an error.

## Tests

- Keep one API stub: `apiStub` is built from `verify/src/fixtures/java/dev/ancaria/coderpack/api/**`.
- `apiKotlinStub` (`templates/src/apiKotlinStub/kotlin`) holds exactly the declarations the Kotlin templates call. A template that starts using one must add it there.
- Generated settings check `mavenLocal()` first, so a locally published API replaces the stubs in the end-to-end test.
- Verifier tests build real jars from fixtures and assert exact findings. Never replace them with mocks.
- `verify/src/legacy/java` holds the API 2 `SacredMod` interface stub; it cannot share a source set with the current stub of the same name.

## Release

- `tools/version.ps1 <new>` moves `version` and `apiVersion` together, plus `Descriptor.kt`'s Javadoc and the READMEs. Edit `gradle/gradle.properties` by hand when the two must diverge.
- Publish `plugin`, `verify`, and `templates` together at one version. The plugin POM needs `verify`; `idea` needs `templates`.
- `nmcpPublishAggregationToCentralPortal` sends `verify` and `templates` to Maven Central. `:plugin:publishPlugins` sends the plugin to the Gradle Plugin Portal, the only place `id("dev.ancaria.coderpack")` resolves from. `nmcpZipAggregation` builds the Central bundle locally.
- Secrets: `CENTRAL_USERNAME` and `CENTRAL_PASSWORD` (a Sonatype user token), `SIGNING_KEY`, `SIGNING_PASSWORD`, `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`. None is needed to build; without a signing key signing is skipped.
