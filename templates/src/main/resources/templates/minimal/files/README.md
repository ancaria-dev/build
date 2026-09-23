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
│   └── {{class}}.{{srcExt}}     entrypoint: onLoad runs once
├── {{buildFile}}                sacred { } block: id, displayName, entrypoint, version
├── {{settingsFile}}
├── registry.toml                SRML metadata, read by `coderpack index`
├── .github/workflows/build.yml  builds, verifies, and releases on a version tag
├── dependencies.json            the coderpack version that workflow downloads
└── gradlew, gradlew.bat, gradle/wrapper/, .gitignore
```

`{{class}}` extends `SacredMod`. The loader creates it once through its
no-argument constructor and calls `onLoad()`. `getContext()` returns the mod’s
`Context` from the first line of the class on: `log`, `print`, `getGame()`,
`getDescriptor()`, and `getRegistry()`. `onUnload()` runs when the mod is
unregistered or the loader shuts down. This minimal template registers no
listeners. It suits a mod that only uses `getContext().getGame()`, as well as
one that keeps its listeners in separate classes.

To receive events, pass an object to
`getContext().getRegistry().getEventRegistry().register(...)`. Each listener
must be a public method marked with `@Subscribe` and accept exactly one event
parameter. The parameter’s type determines which events the method receives.
The method returns `void` to observe. On an event that can be decided, such as
`Gold` or `Damage`, it can instead return that event’s own `Mutation`, for
example `Gold.Mutation.change(...)`. Events are read-only, so this is the only
way to change the outcome. A `MONITOR` listener must return `void`.

The same event registry also takes a lambda: `on(Hero.class, hero -> ...)`
observes, and `decide(Gold.class, gold -> Gold.Mutation.change(...))` decides.
In Kotlin the API’s getters read as properties, so the same call is
`context.registry.eventRegistry.on(Hero::class.java) { hero -> }`.
