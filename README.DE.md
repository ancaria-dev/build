<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Shadow](https://img.shields.io/badge/Shadow-9.6.1-1E88E5?style=for-the-badge)](https://gradleup.com/shadow/)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[Русский](README.md) · [English](README.EN.md)

</div>

# build

Build-Werkzeuge für Mods für Sacred Gold: ein Gradle-Plugin, ein Linter und
das Kommando `coderpack`, das neue Projekte anlegt.

Du beschreibst deinen Mod in einem einzigen Block `sacred { }`. Das Plugin
schreibt den Mod-Deskriptor, packt deinen Code samt Bibliotheken in ein JAR und
prüft es, bevor es irgendwohin wandert. Ein JAR, das der Loader ablehnen würde,
landet nie in deinem Spielordner.

Hier geht es um den Build. Die Mod-API mit ihren Ereignissen und Listenern
beschreibt [coderpack](https://github.com/ancaria-dev/coderpack).

## Erste Schritte

Du brauchst ein JDK. Gradle bringt das Projekt selbst mit.

1. Lade `coderpack-<Version>.zip` aus den
   [Releases](https://github.com/ancaria-dev/build/releases) herunter, entpack
   es und nimm den Ordner `bin` in den PATH auf.
2. Leg ein Projekt an und bau es:

   ```
   coderpack new my-mod
   cd my-mod
   gradlew assembleSacredMod
   ```

3. Kopier das JAR aus `build/sacred-mod/` nach `<Sacred Gold>/mods`, oder
   überlass das Gradle:

   ```
   gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
   ```

Lieber in der IDE? Das
[Plugin für IntelliJ IDEA](https://github.com/ancaria-dev/idea) legt dasselbe
Projekt über File → New → Project an.

## Ein neues Projekt

Bei `coderpack new my-mod` wird `my-mod` zur Mod-ID. Das Kommando prüft sie,
bevor es etwas schreibt, und leitet daraus den Anzeigenamen `My Mod`, das
Paket `mods.mymod` und die Klasse `MyMod` ab. Das Projekt enthält:

```
.gitignore
README.md
build.gradle.kts                     der ausgefüllte Block sacred { }
settings.gradle.kts                  zuerst mavenLocal, dann das Plugin-Portal
registry.toml                        Angaben für ein Mod-Repository
.github/workflows/build.yml          baut und veröffentlicht jede neue Version
dependencies.json                    die coderpack-Version für die CI
gradlew, gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/mods/mymod/MyMod.java  onLoad und ein funktionierender Listener
```

Der Wrapper entspricht dem dieses Repositorys: Gradle 9.7.1 mit derselben
SHA-256-Prüfsumme. Dein erster Build braucht also nur einen Befehl, egal
welches Gradle auf dem Rechner installiert ist.

| Option | Was sie ändert |
|---|---|
| `--package dev.example.mymod` | Das Paket und den Ordner des Einstiegspunkts |
| `--display-name "My Splendid Mod"` | Den Namen, den Spieler sehen |
| `--author "Your Name"` | Den Wert `authors`. Standard ist das aktuelle Konto |
| `--description "One sentence"` | Die Zeile unter dem Mod-Namen |
| `--mod-version 0.2.0` | Die Mod-Version. Standard ist `1.0.0` |
| `--dir somewhere/else` | Den Zielordner. Standard ist `./<name>` |
| `--template minimal` | Das Template: `mod` (Standard) oder `minimal` |
| `--language kotlin` | Die Mod-Sprache: `java` (Standard), `kotlin` oder `groovy` |
| `--dsl groovy` | Die Sprache des Build-Skripts: `kotlin` (Standard) oder `groovy` |
| `--repo https://github.com/me/my-mod` | Die Projekt-URL für Download-Links |
| `--no-registry` | Lässt `registry.toml`, `dependencies.json` und den Workflow weg |
| `--git` | Führt `git init` aus und trägt `--repo` als `origin` ein |
| `--force` | Schreibt in einen Ordner, der nicht leer ist |

Jede Sprache passt zu jeder DSL. `coderpack templates` zeigt, was es gibt, und
`--help` funktioniert bei jedem Befehl. Ein Kotlin-Projekt bekommt zusätzlich
`dev.ancaria.coderpack:api-kotlin`, optionale Kotlin-Erweiterungen, die
[coderpack](https://github.com/ancaria-dev/coderpack) beschreibt.

`--git` läuft erst, wenn alle Dateien geschrieben sind. Fehlt Git, bleibt das
Projekt trotzdem nutzbar. Den ersten Commit macht das Kommando nie für dich.

## Der Block sacred

`build.gradle.kts` beschreibt den Mod:

```kotlin
plugins {
    id("dev.ancaria.coderpack") version "0.200.0"
}

version = "1.0.0"

dependencies {
    // Alles, was der Mod zur Laufzeit braucht. Es landet im JAR.
    implementation("org.jetbrains:annotations:26.0.2")
}

sacred {
    id = "my-mod"                                     // Kleinbuchstaben, Ziffern, Bindestriche
    displayName = "My Mod"                            // was Spieler lesen
    description = "One sentence, shown under the name in the mod list"
    version = "1.0.0"                                 // Standard ist die Projektversion
    entrypoint = "demo.MyMod"                         // die Klasse, die SacredMod erweitert
    authors = listOf("MairwunNx (Pavel Erokhin)")     // oder author("…") einzeln
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    conflicts = listOf("other-mod")                   // oder conflictsWith("other-mod")
    apiRange = "[3,4)"                                // unterstützte API-Verträge
    loaderRange = "[0.1.20,)"                         // optionaler Bereich von Loader-Versionen
    apiVersion = "0.200.0"                            // wird als compileOnly ergänzt
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
```

Pflicht sind nur `id` und `entrypoint`. `displayName` fällt auf `id` zurück,
und leere optionale Werte bleiben aus dem Deskriptor draußen.

Drei Einstellungen klingen ähnlich, meinen aber Verschiedenes:

- `apiRange` nennt die API-Verträge, mit denen der Mod läuft, in der
  Bereichsnotation von Maven. `[3,4)` heißt Vertrag 3 bis ausschließlich 4.
  Du darfst den Bereich weiten oder enger fassen, aber Vertrag 3, für den
  diese Werkzeuge bauen, muss drinbleiben.
- `loaderRange` grenzt Versionen des Sacred Mod Loader ein. Setz es nur, wenn
  dein Mod eine bestimmte Version braucht. Das Plugin prüft die Syntax, nicht
  ob es diese Version gibt.
- `apiVersion` ist die Maven-Version von `dev.ancaria.coderpack:api`. Das
  Plugin fügt sie als `compileOnly` hinzu.

`installTo` geht auch als fester Pfad:
`installTo = file("D:/SteamLibrary/steamapps/common/Sacred Gold/mods")`.

| Task | Was er macht |
|---|---|
| `generateModDescriptor` | Schreibt `META-INF/declaration.toml` aus dem Block `sacred` |
| `verifySacredMod` | Prüft das gepackte JAR und schreibt `build/reports/sacred-mod/verify.txt` |
| `assembleSacredMod` | Kopiert das geprüfte JAR nach `build/sacred-mod` |
| `installSacredMod` | Kopiert dieses JAR nach `installTo` |

Alles unter `implementation` landet im JAR, die Laufzeit der Sprache
eingeschlossen. Aus dem Template `mod` wiegt ein Java-JAR etwa 1,8 KB, ein
Kotlin-JAR etwa 1,8 MB und ein Groovy-JAR etwa 7,8 MB. Alle Compiler zielen
auf Java 21.

## Was der Linter prüft

`assembleSacredMod` startet zuerst `verifySacredMod`. Ein Fehler stoppt den
Build. Als Fehler gilt:

- ein unlesbares JAR oder eines ohne `META-INF/declaration.toml`
- ein Deskriptor ohne `id`, `entrypoint` oder `api`
- ein unlesbarer API- oder Loader-Bereich oder ein API-Bereich ohne Vertrag 3
- ein fehlender oder abstrakter Einstiegspunkt oder einer, der `SacredMod`
  nicht erweitert
- ein Einstiegspunkt, der `SacredMod` noch als Interface aus API 2
  implementiert
- ein Einstiegspunkt ohne Konstruktor ohne Argumente
- Klassen der Loader-API, die im Mod mitgepackt sind
- eine Methode mit `@Subscribe`, die nicht genau einen Ereignisparameter hat,
  deren Rückgabetyp weder `void` noch die `Mutation` genau dieses Ereignisses
  ist, oder eine `MONITOR`-Methode, die eine Mutation zurückgibt
- Signaturdateien aus einer signierten Bibliothek

Warnungen lassen den Build weiterlaufen. Sie melden eine ungültige ID, eine
fehlende Version, einen ungültigen Konflikt oder einen Konflikt mit sich
selbst, mitgepackte Zygote-Klassen, Class-Dateien neuer als Java 21, einen
Einstiegspunkt mit Elternklasse außerhalb des JARs sowie einen nicht
öffentlichen Einstiegspunkt, Konstruktor oder Listener.

Ein fehlgeschlagener Build sieht so aus:

```
> Task :verifySacredMod FAILED
...
> my-mod-1.0.0.jar: 1 error
    error   listener    demo.MyMod.onHero takes 2 parameters. The bus registers a listener with exactly one, and the parameter is what it subscribes to
```

Dieselben Prüfungen laufen auch ohne Gradle. `coderpack verify` gibt einen
Bericht pro JAR aus und endet mit `1` bei einem Fehler oder `2` bei falschen
Argumenten:

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

Der Linter liest Bytecode mit ASM und lädt deine Klassen nie. Im Build läuft
also kein statischer Initialisierer.

## Einen Mod veröffentlichen

Ohne `--no-registry` ist jedes neue Projekt gleich auch ein Mod-Repository,
das der Launcher abonnieren kann:

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
gradlew assembleSacredMod
coderpack index
```

`registry.toml` pflegst du selbst:

```toml
name = "My Mod"
description = "My Mod, a mod for Sacred Gold"
url = "https://github.com/me/my-mod"
releases = "https://github.com/me/my-mod/releases/download/{id}-v{version}/{file}"
```

Pflicht sind `name`, `url` und `releases`. `description` und `icon` sind
optional. Die Vorlage `releases` baut jeden Download-Link und muss `{file}`
enthalten. Ohne `--repo` schreibt das Kommando `https://github.com/you/my-mod`
und sagt dir, welche Zeile du ändern musst.

`coderpack index` verbindet `registry.toml` mit dem Deskriptor jedes gebauten
JARs und schreibt `sacred.mods.repository.json`. Bearbeite diese Datei nicht
von Hand. Sie enthält keinen Zeitstempel, deshalb kann
`coderpack index --check` sie Byte für Byte vergleichen.

Commit und push. Der mitgelieferte Workflow baut den Mod, erzeugt den Index
neu, committet ihn und legt ein Release `<id>-v<Version>` an, wenn es diesen
Tag noch nicht gibt. Ein Push ohne neue Version veröffentlicht nichts. Außer
dem GitHub-eigenen Token braucht der Workflow kein Secret.

## Warum der Build so aufgebaut ist

**Das Plugin schreibt den Deskriptor.** Version und Co. kennt der Build
ohnehin. Ein erzeugter Deskriptor kann nicht vom JAR abweichen.

**Bibliotheken wandern in ein JAR.** Jeder Mod hat seinen eigenen Classloader,
und der sieht nur, was im JAR liegt. Das Packen übernimmt Shadow.

**Die API gibt es nur zum Kompilieren.** Der Loader bringt sie schon mit. Eine
zweite Kopie erzeugt verschiedene Klassen mit gleichen Namen und kann in einer
`ClassCastException` enden.

**Das Plugin wendet nur `java` an.** Die Plugins für Kotlin und Groovy bauen
darauf auf, also wählt und konfiguriert dein Mod seine Sprache selbst.

## Aufbau des Repositorys

```
build/
  gradle/      der Gradle-Build
    plugin/    SacredPlugin, SacredExtension, Descriptor, Verify und Tests
  verify/      der Linter, in Java
  templates/   coderpack, Projekt-Templates, Sprachen und DSLs
  maven/       Notizen zu einem möglichen Maven-Plugin, noch nicht umgesetzt
```

Gemeinsame Regeln liegen in `verify/`. `Ids.valid` legt fest, was eine gültige
Mod-ID ist, und Plugin wie Kommando rufen es auf. `Verifier.API` hält den
API-Vertrag, und `Verifier.API_RANGE` leitet daraus den Standardbereich ab.

Templates, Sprachen und DSLs sind Ressourcenordner unter
`templates/src/main/resources/`: `templates/<name>/`, `languages/<name>/` und
`dsl/<name>/`. Jede Datei liegt dort, wo sie variiert: Ein Einstiegspunkt
hängt von Template und Sprache ab, ein Build-Skript von Sprache und DSL, eine
Settings-Datei nur von der DSL. So ergeben 27 Dateien 12 Kombinationen. Der
Build indiziert die Ordner selbst, ein neuer Ordner braucht also keinen
Kotlin-Code.

Das Kommando ersetzt `{{id}}`, `{{name}}`, `{{description}}`, `{{version}}`,
`{{package}}`, `{{packagePath}}`, `{{class}}`, `{{entrypoint}}`,
`{{author}}`, `{{repo}}`, `{{plugin}}` und `{{api}}` sowie jeden Schlüssel
außer `description` aus `language.properties` und `dsl.properties`. Ein
offener Platzhalter bricht den Lauf ab. Für eine `.gitignore` nennst du die
Datei `_gitignore`: Beim Kopieren von Ressourcen fallen Dateien mit Punkt weg.

Dasselbe Modul erscheint als `dev.ancaria.coderpack:templates`. Das Plugin für
IntelliJ IDEA ruft es auf und führt keine eigene Kopie der Templates.

## Bauen

```
cd gradle
./gradlew build
```

Du brauchst ein JDK im PATH. Die CI nutzt Temurin 21. Einen Toolchain-Block
gibt es nicht: Das JDK, das Gradle startet, kompiliert alles für Java 21. Der
Konfigurations-Cache ist an und schlägt bei jedem Problem fehl. Ein Task, der
`project` festhält, bricht also hier und nicht erst in einem fremden Mod.

Die Tests bauen echte Projekte und echte JARs. Die Plugin-Tests nutzen Gradle
TestKit. Der End-to-End-Test des Kommandos erzeugt und baut alle sechs Paare
aus Sprache und DSL und erwartet einen Bericht ohne eine einzige Warnung. Die
Linter-Tests bauen pro Regel ein eigenes JAR und laden dabei keine Klasse.
`./gradlew build` veröffentlicht Plugin und Linter außerdem in Maven Local,
weil der End-to-End-Test sie so bezieht wie ein erzeugter Mod.

Das Kommando aus den Quellen probierst du so aus:

```
./gradlew :templates:installDist :verify:demoModJar
../templates/build/install/coderpack/bin/coderpack verify ../verify/build/demo/demo-mod.jar
../templates/build/install/coderpack/bin/coderpack new demo-mod
```

Willst du unveröffentlichte Änderungen an einem Mod auf demselben Rechner
testen, führ `./gradlew publishToMavenLocal` hier und in `coderpack` aus.
Erzeugte Projekte schauen zuerst in Maven Local.

## Releases

Die Version steht in `gradle/gradle.properties`. `pwsh tools/version.ps1
0.200.1` hebt sie zusammen mit `apiVersion` und jeder Erwähnung in den READMEs
an. Landet eine neue Version auf `master`, veröffentlicht die CI sie an zwei
Orten:

- Linter und Kommando gehen als ein signiertes Paket nach Maven Central
- das Plugin geht ins Gradle Plugin Portal, den einzigen Ort, aus dem
  `id("dev.ancaria.coderpack")` aufgelöst wird

Danach legt die CI den Tag `v<Version>` an und hängt `coderpack-<Version>.zip`
an das GitHub-Release. `plugin`, `verify` und `templates` teilen sich immer
eine Version: Das Plugin hängt vom Linter ab, das IDE-Plugin von den
Templates.

Central braucht `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY` und
`SIGNING_PASSWORD`, das Portal `GRADLE_PUBLISH_KEY` und
`GRADLE_PUBLISH_SECRET`. Ein Upload nach Central wartet, bis jemand im Portal
auf Publish drückt. Die Release-Reihenfolge des ganzen Loaders steht in
[CONTRIBUTING](https://github.com/ancaria-dev/.github/blob/master/CONTRIBUTING.DE.md).

## Lizenz

MIT, siehe [LICENSE](LICENSE).
