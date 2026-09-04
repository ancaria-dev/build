# {{name}}

[![Sacred Community](https://img.shields.io/badge/Sacred-Community-C8912B?style=flat-square)](https://ancaria.dev)

{{description}}

A Sacred Gold mod by {{author}}.

## Build the mod

```
./gradlew assembleSacredMod
```

Gradle writes `build/sacred-mod/{{id}}-{{version}}.jar`. The jar contains the
mod and its runtime dependencies. Before copying it there, the build checks the
packed jar against the loader’s requirements.

## Install the mod

```
./gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
```

This copies the assembled jar into `<Sacred Gold>/mods`. Enable the mod in the
launcher, then start the game.

## Project layout

```text
{{id}}/
├── src/main/{{srcDir}}/{{packagePath}}/
│   └── {{class}}.{{srcExt}}     entrypoint -- onLoad runs once
├── {{buildFile}}                sacred { } block: id, displayName, entrypoint, version
├── {{settingsFile}}
├── registry.toml                SRML metadata, read by `coderpack index`
├── .github/workflows/build.yml  builds, verifies, and releases on a version tag
└── gradlew, gradlew.bat, gradle/wrapper/, .gitignore
```

The loader calls `{{class}}.onLoad` once and passes it a `Context`. This minimal
template registers no listeners. It suits a mod that only uses
`context.game()`, as well as one that keeps its listeners in separate classes.

To receive events, pass an object to `context.events().register(...)`. Each
listener must be a public method marked with `@Subscribe` and accept exactly one
event parameter. It must return no value. The parameter’s type determines which
events the method receives.
