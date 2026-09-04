# {{name}}

[![Sacred Community](https://img.shields.io/badge/Sacred-Community-C8912B?style=flat-square)](https://ancaria.dev)

{{description}}

A Sacred Gold mod by {{author}}.

## Build the mod

```
./gradlew assembleSacredMod
```

This creates `build/sacred-mod/{{id}}-{{version}}.jar` with the mod’s runtime
dependencies inside. Coderpack checks the jar against the loader’s requirements
before copying it there. If the check finds an error, the build stops.

## Install and play

```
./gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
```

This copies the jar into `<Sacred Gold>/mods`. Select the mod in the launcher,
then start the game.

## Project layout

```text
{{id}}/
├── src/main/{{srcDir}}/{{packagePath}}/
│   └── {{class}}.{{srcExt}}     entrypoint -- onLoad runs once, register listeners here
├── {{buildFile}}                sacred { } block: id, displayName, entrypoint, version
├── {{settingsFile}}
├── registry.toml                SRML metadata, read by `coderpack index`
├── .github/workflows/build.yml  builds, verifies, and releases on a version tag
└── gradlew, gradlew.bat, gradle/wrapper/, .gitignore
```

`{{class}}.onLoad` runs once and receives the mod context. Register listeners
there. A listener is a public `@Subscribe` method with one event parameter and
no return value. The parameter type determines which events it receives.

The world does not exist when `onLoad` runs. Wait for a `Hero` event or
`World.Phase.LOADED` before touching player state.
