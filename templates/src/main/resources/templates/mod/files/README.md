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
│   └── {{class}}.{{srcExt}}     entrypoint: onLoad runs once, register listeners here
├── {{buildFile}}                sacred { } block: id, displayName, entrypoint, version
├── {{settingsFile}}
├── registry.toml                SRML metadata, read by `coderpack index`
├── .github/workflows/build.yml  builds, verifies, and releases on a version tag
├── dependencies.json            the coderpack version that workflow downloads
└── gradlew, gradlew.bat, gradle/wrapper/, .gitignore
```

`{{class}}` is loaded once and handed the mod context. Register listeners
there. In Java and Groovy a listener is a public `@Subscribe` method with one
event parameter, and the parameter type decides which events it receives. It
returns `void` to observe. On an event that can be decided, such as `Gold` or
`Damage`, it can instead return that event's own `Mutation`, for example
`Gold.Mutation.change(...)`. Events are read-only, so the returned mutation is
the only way to change the outcome. A `MONITOR` listener must return `void`.

The Kotlin entrypoint registers the same listener as `on<Hero> { }`, through
`dev.ancaria.coderpack:api-kotlin`: the same bus, said in Kotlin. There a
listener decides with `mutate { Gold.Mutation.change(...) }` inside the
`on<Gold> { }` body. `@Subscribe` still works there, and the extensions are
still optional.

The world does not exist yet at that point. Wait for a `Hero` event or
`World.Phase.LOADED` before touching player state.
