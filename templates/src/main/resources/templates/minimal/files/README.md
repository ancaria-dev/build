# {{name}}

{{description}}

The Sacred Mod Loader loads {{name}} while Sacred Gold is running. Created by
{{author}}.

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

```
{{buildFile}}   the sacred { } block: id, displayName, entrypoint, version
src/main/{{srcDir}}/{{packagePath}}/{{class}}.{{srcExt}}
```

The loader calls `{{class}}.onLoad` once and passes it a `Context`. This minimal
template registers no listeners. It suits a mod that only uses
`context.game()`, as well as one that keeps its listeners in separate classes.

To receive events, pass an object to `context.events().register(...)`. Each
listener must be a public method marked with `@Subscribe` and accept exactly one
event parameter. It must return no value. The parameter’s type determines which
events the method receives.
