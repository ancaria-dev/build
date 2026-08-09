# Maven support

There is no Maven plugin in this repository. The directory contains this note
only, and implementation is deferred until a mod author needs a `pom.xml`
workflow.

A Maven equivalent of the Gradle plugin would need:

- a `generate-descriptor` goal bound to `process-resources`, writing
  `META-INF/declaration.toml` to `target/classes` from a `<sacred>`
  configuration block
- shaded packaging through `maven-shade-plugin`, with the API dependency scoped
  as `provided`
- Java compilation targeting release 21
- verification of the shaded package before it can be copied to a mod folder
- an optional `install-mod` goal that copies the package into the game's mods
  folder
