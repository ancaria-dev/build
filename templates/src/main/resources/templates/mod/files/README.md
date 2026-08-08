# {{name}}

{{description}}

A Sacred Gold mod by {{author}}. It loads while the game is running.

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

```
{{buildFile}}   the sacred { } block: id, displayName, entrypoint, version
src/main/{{srcDir}}/{{packagePath}}/{{class}}.{{srcExt}}
```

`{{class}}.onLoad` runs once and receives the mod context. Register listeners
there. A listener is a public `@Subscribe` method with one event parameter and
no return value. The parameter type determines which events it receives.

The world does not exist when `onLoad` runs. Wait for a `Hero` event or
`World.Phase.LOADED` before touching player state.
