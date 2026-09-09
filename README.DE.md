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

Dieses Repository enthält Build-Unterstützung für Mods von Sacred Gold. Jedes
Build-System bekommt ein eigenes Verzeichnis, sodass später weitere
hinzukommen können, ohne voneinander abzuhängen.

Derzeit gibt es die Gradle-Variante. Unter `gradle/` liegt das Plugin mit der ID
`dev.ancaria.coderpack`. `maven/` enthält bislang nur eine Notiz zum möglichen
Aufbau eines Maven-Plugins.

`verify/` gehört zu keinem Build-System. Der Linter untersucht ein gepacktes
Mod-JAR und meldet Probleme, bevor die Datei beim Spieler landet. Das
Gradle-Plugin bindet diese Prüfung in `build`, `assembleSacredMod` und
`installSacredMod` ein. CI kann denselben Linter direkt auf einen JAR-Pfad
ansetzen, ganz ohne Gradle.

Unter `templates/` entsteht das Kommandozeilenprogramm `coderpack`. Es legt
neue Mod-Projekte an, prüft fertige JARs und erzeugt den Index für ein
Mod-Repository. Dasselbe Modul wird als
`dev.ancaria.coderpack:templates` veröffentlicht und vom IntelliJ-IDEA-Plugin
verwendet.

## Einen Mod anlegen

```
coderpack new my-mod
cd my-mod
gradlew assembleSacredMod
```

Der letzte der drei Befehle erzeugt ein JAR, das der Loader verwenden kann.
Vorher muss nur ein JDK installiert sein. Build-Dateien aus fremden Projekten
werden nicht kopiert.

Jedes Release enthält das Werkzeug als ZIP-Datei. In einem Checkout lässt es
sich mit einer Gradle-Task bereitstellen:

```
cd gradle
./gradlew :templates:installDist
../templates/build/install/coderpack/bin/coderpack new my-mod
```

Was in `my-mod/` landet:

```
.gitignore
README.md
build.gradle.kts                     der ausgefüllte sacred { }-Block
settings.gradle.kts                  erst mavenLocal, dann das Plugin-Portal
registry.toml                        damit der Mod aus einem Launcher installierbar ist
.github/workflows/build.yml          baut ihn und veröffentlicht jede neue Version
dependencies.json                    die coderpack-Version, die CI lädt
gradlew, gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/mods/mymod/MyMod.java  onLoad, und ein Listener, der feuert
```

Standardmäßig wird ein Java-Projekt erzeugt. Mit `--language kotlin` lautet der
letzte Pfad `src/main/kotlin/mods/mymod/MyMod.kt`, und das Build-Skript wendet
zusätzlich das Kotlin-Plugin an. `--language groovy` erzeugt entsprechend eine
`.groovy`-Datei.

Ein Kotlin-Projekt bekommt eine Abhängigkeit, die die beiden anderen nicht
haben: `dev.ancaria.coderpack:api-kotlin`, dieselbe Loader-API in Kotlin. Der
Einstiegspunkt erbt von der Klasse `SacredMod` aus diesem Modul, die den
Context festhält und ihn `Context.load()` als Receiver übergibt, und meldet
seinen Listener als `on<Hero> { }` statt als annotierte Methode an. Das Modul
kann nichts, was die Java-API nicht kann: jede Deklaration ruft eine Methode
dort auf, und ein Feld, das die API überschreiben lässt, ist ein `var`, sodass
eine Änderung `it.delta *= 2` lautet. Die Abhängigkeit zu streichen und
stattdessen `@Subscribe` zu schreiben, ist ein unterstützter Weg zu einem
Kotlin-Mod. Eingebunden wird es als `implementation`, nicht als `compileOnly`,
weil der Loader einem Mod die API reicht und dieses Modul nicht.

Die Sprache des Build-Skripts wird unabhängig davon gewählt. `--dsl groovy`
schreibt `build.gradle` und `settings.gradle` anstelle der beiden
`.kts`-Dateien. Ein Java-Mod kann also mit Groovy DSL gebaut werden, ein
Kotlin-Mod mit Kotlin DSL. Ohne `--dsl` verwendet `coderpack` Kotlin DSL.

Mit `--no-registry` entfallen `registry.toml`, `dependencies.json` und der
Workflow für Releases. Standardmäßig entsteht dagegen ein vollständiges
Mod-Repository, das der Launcher abonnieren kann.

`my-mod` ist die Mod-ID. Noch vor dem ersten Schreibzugriff prüft `coderpack`
sie nach derselben Regel wie das Plugin. Aus der ID leitet das Werkzeug den
Anzeigenamen `My Mod`, das Paket `mods.mymod` und die Einstiegsklasse `MyMod`
ab. Diese Werte lassen sich über Optionen ändern.

| Option | Was sie ändert |
|---|---|
| `--package dev.example.mymod` | das Java-Paket und damit das Verzeichnis des Einstiegspunkts |
| `--display-name "Mein Prachtmod"` | der Name, den ein Spieler liest |
| `--author "Dein Name"` | die `authors`-Zeile im Deskriptor, sonst das Konto, das den Befehl ausführt |
| `--description "Ein Satz"` | was die Mod-Liste unter dem Namen zeigt |
| `--mod-version 0.2.0` | die eigene Version des Mods, sonst 1.0.0 |
| `--dir woanders/hin` | wohin geschrieben wird, sonst `./<name>` |
| `--template minimal` | welches Template, siehe unten |
| `--language kotlin` | worin der Mod selbst geschrieben ist: `java`, `kotlin` oder `groovy`, sonst `java` |
| `--dsl groovy` | worin der Build des Projekts geschrieben ist: `kotlin` oder `groovy`, sonst `kotlin` |
| `--repo https://github.com/me/my-mod` | wo das Projekt leben wird: daraus entsteht jeder Download-Link |
| `--no-registry` | das Projekt nicht zum Mod-Repository machen: keine `registry.toml`, keine `dependencies.json`, kein Release-Workflow |
| `--git` | `git init` ausführen und `--repo` als `origin` eintragen |
| `--force` | in ein Verzeichnis schreiben, in dem schon etwas liegt |

**Das Projekt bringt seinen Gradle Wrapper mit**, einschließlich Wrapper-JAR.
Dadurch genügt ein Befehl, auch wenn auf dem Rechner kein passendes
System-Gradle installiert ist. Der Wrapper liegt nicht als zweite, getrennt zu
pflegende Kopie im Scaffolder. Beim Build werden `gradlew`, `gradlew.bat` und
`gradle/wrapper/` aus diesem Repository als Ressourcen eingebunden. Ein
erzeugtes Projekt lädt damit Gradle 9.7.1 und prüft die Distribution anhand
derselben SHA-256-Prüfsumme wie dieses Repository.

### Einen installierbaren Mod veröffentlichen

Das erzeugte Projekt ist zugleich ein Mod-Repository. Dadurch erscheint der Mod
im Launcher mit einer Installationsschaltfläche, statt nur als lose JAR-Datei
vorzuliegen. Diese vier Befehle genügen, handgeschriebenes JSON braucht es
nicht:

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
gradlew assembleSacredMod
coderpack index
```

Bei einem Push auf `main` oder `master` sowie bei einem Pull Request baut der
mitgelieferte Workflow den Mod, erzeugt den Index aus dem soeben gebauten JAR
neu und committet ihn. Verglichen wird nichts: ein JAR, das eine Sprachlaufzeit
mitbringt, fällt von Rechner zu Rechner nicht Byte für Byte gleich aus, und ein
Vergleich würde an korrekter Arbeit scheitern. Nur auf dem Default-Branch
veröffentlicht er ein Release mit dem Tag
`<id>-v<version>`, sofern dieser Tag noch nicht existiert. Zusätzliche Secrets
sind nicht nötig, weil der von GitHub bereitgestellte Token Releases im eigenen
Repository anlegen darf. Solange sich die Version nicht ändert, wird kein neues
Release erzeugt.

`coderpack index` schreibt `sacred.mods.repository.json` neben
`registry.toml`. Die Angaben zum Mod stammen aus dem Deskriptor des gebauten
JARs. Jeder Eintrag kann Name, Beschreibung, Version, API- und Loader-Bereich,
Autoren, Website, Konflikte, Quellpfad und Icon enthalten. Dazu kommen
Dateiname, Größe, SHA-256 und Download-URL. Im Wurzelobjekt steht das Feld
`srml` auf `1`, neben Repository-Name, Beschreibung, URL, Icon und Mod-Liste. Ein
Zeitstempel fehlt absichtlich, damit `coderpack index --check` für einen
gegebenen Satz JAR-Dateien die Ausgabe Byte für Byte vergleichen kann. Beide Dateien gehören in den Commit.

Nur `registry.toml` wird von Hand gepflegt:

```toml
name = "My Mod"
description = "My Mod, a mod for Sacred Gold"
url = "https://github.com/me/my-mod"
releases = "https://github.com/me/my-mod/releases/download/{id}-v{version}/{file}"
```

`name`, `url` und `releases` sind Pflicht. `description` ist optional, ebenso
ein Feld `icon` für das Repository-Icon. Die Vorlage `releases` erzeugt die
Download-URLs und muss `{file}` enthalten. Mit `--repo` setzt der Scaffolder
beide URL-Felder. Ohne die Option steht dort gut sichtbar
`https://github.com/you/my-mod`, und der Bericht nennt die zu ändernde Zeile.

`--git` führt nach dem Schreiben aller Dateien `git init` aus und trägt
`--repo` als `origin` ein. Fehlt Git auf dem Rechner, bleibt das erzeugte
Projekt verwendbar und der Bericht weist darauf hin. Einen Commit legt der
Scaffolder nicht an.

### Templates, Sprachen und Build-Skripte

```
coderpack templates
templates:
  minimal   An entrypoint and nothing else. For a mod that listens to nothing.
            groovy, java, kotlin
  mod       An entrypoint and one working listener. The default.
            groovy, java, kotlin

languages:
  groovy    Groovy, with the runtime packed into the jar.
  java      Plain Java. What new writes when nobody says otherwise.
  kotlin    Kotlin, with the standard library packed into the jar.

build scripts:
  groovy    Groovy DSL. build.gradle, the older syntax most Gradle answers are written in.
  kotlin    Kotlin DSL. build.gradle.kts, with completion and refactoring in the IDE.
```

Ein Template liegt unter
`templates/src/main/resources/templates/<name>/`. `template.properties`
enthält seine Beschreibung. Der Baum `files/` gilt für jedes Projekt dieses
Templates, während `lang/<sprache>/` den jeweiligen Einstiegspunkt enthält.

Sprachen liegen unter `templates/src/main/resources/languages/<name>/`.
`language.properties` beschreibt die Sprache. `files/` enthält
sprachspezifische Dateien, die unabhängig von der Build-Syntax gelten. Dieser
Baum ist derzeit leer. Unter `dsl/<dsl>/` liegen die Build-Skripte.

Eine Build-DSL liegt unter `templates/src/main/resources/dsl/<name>/`. Dort
stehen `dsl.properties` und die zugehörige Settings-Datei im Baum `files/`.
Sprache und DSL sind zwei getrennte Entscheidungen. Die Sprache bestimmt den
Quelltext und den Bytecode des Mods. Die DSL bestimmt nur die Syntax seiner
Build-Dateien.

Einstiegspunkte unterscheiden sich nach Template und Sprache. Build-Skripte
hängen von Sprache und DSL ab, Settings-Dateien nur von der DSL. README,
`.gitignore` und SRML-Dateien sind in allen zwölf Kombinationen gleich und
werden deshalb einmal gepflegt. So entstehen zwölf funktionsfähige Projekte aus
26 Dateien statt aus 72 nahezu gleichen Kopien.

Die Platzhalter `{{id}}`, `{{package}}`, `{{packagePath}}`, `{{class}}`,
`{{entrypoint}}`, `{{name}}`, `{{description}}`, `{{author}}`, `{{repo}}`,
`{{version}}`, `{{plugin}}` und `{{api}}` werden in Dateiinhalten und
Dateinamen ersetzt. Zusätzliche Werte kommen aus `language.properties` und
`dsl.properties`. Jeder Schlüssel außer `description` wird ebenfalls zum
Platzhalter. Deshalb kann ein gemeinsames README etwa `src/main/kotlin` und
`build.gradle.kts` nennen, während das Kotlin-Build-Skript seine
Kotlin-Version erhält. Bleibt ein Platzhalter offen, bricht der Vorgang ab.

Für ein weiteres Template, eine weitere Sprache oder eine neue Build-DSL genügt
die passende Verzeichnisstruktur. Der Build indexiert `src/main/resources`,
und `coderpack` liest diesen Index zur Laufzeit. Kotlin-Code muss dafür nicht
angepasst werden. Eine Besonderheit betrifft Dateinamen mit führendem Punkt:
`_gitignore` wird im erzeugten Projekt zu `.gitignore`, weil der
Kopiervorgang solche Dateien sonst auslassen würde.

Eine unbekannte Sprache wird vor dem ersten Schreibzugriff abgelehnt. Dasselbe
gilt für unbekannte Optionen und Build-DSLs:

```
coderpack: there is no language "rust". There is: groovy, java, kotlin
```

`--help` und `-h` funktionieren bei jedem Unterbefehl und geben dessen Optionen
aus. `coderpack new --help` zeigt dieselbe Optionsliste wie `coderpack help`,
nur ohne die übrige Hilfe.

### Ein JAR prüfen

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

Der Befehl verwendet dieselbe Bibliothek wie die Task `verifySacredMod`, aber
direkt auf einem Dateipfad und ohne Gradle. So prüft ein Mod-Repository ein
Release. Die CI dieses Projekts führt denselben Test bei jedem Push mit einem
gepackten Fixture aus.

## Einen Mod schreiben

`coderpack new` schreibt die beiden folgenden Dateien.

`settings.gradle.kts` legt fest, woher das Plugin und die Loader-API kommen:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "my-mod"
```

`mavenLocal()` steht an erster Stelle, weil `publishToMavenLocal` das Plugin und
die API dort ablegt. Solange noch kein Release veröffentlicht ist, müssen
deshalb sowohl `build` als auch `coderpack` lokal ausgecheckt werden. Führe in
beiden Checkouts `./gradlew publishToMavenLocal` aus. Das Gradle Plugin Portal
und Maven Central sind bereits für spätere Veröffentlichungen eingetragen.

Ein kompaktes `build.gradle.kts` kann mit Beispielwerten so aussehen:

```kotlin
plugins {
    id("dev.ancaria.coderpack") version "0.100.1"
}

version = "1.0.0"

dependencies {
    // Alles, was der Mod zur Laufzeit braucht, gehört hierher und wird in das
    // JAR gepackt. Jeder Mod hat einen eigenen Klassenlader und sieht nur
    // Bibliotheken, die in seinem JAR liegen.
}

sacred {
    id = "my-mod"                                     // Kleinbuchstaben, Ziffern, Bindestriche
    displayName = "My Mod"                            // was Spieler im Launcher sehen
    description = "Ein Satz, unter dem Namen in der Modliste"
    entrypoint = "mods.mymod.MyMod"                   // die Klasse, die SacredMod implementiert
    author("Dein Name")

    // Der Loader stellt diese API bereit. Sie wird zum Kompilieren verwendet,
    // aber nicht in das JAR gepackt.
    apiVersion = "0.100.0"                              // wird als compileOnly ergänzt

    // Ziel für installSacredMod. Der Spieleordner kommt als Property:
    //   gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
```

Nur `id` und `entrypoint` sind Pflicht. Fehlen optionale Angaben, erzeugt das
Plugin trotzdem einen gültigen Deskriptor. Eine ungültige `id` stoppt den Build,
bevor ein JAR entsteht, das der Launcher später nicht auflisten könnte.

Ohne eigene Angabe schreibt das Plugin `api = "[1,2)"` in den Deskriptor. Das
ist der Bereich der API-Verträge, für die der Mod gebaut wurde. `apiRange` darf
enger oder weiter gefasst werden, muss aber den Vertrag dieser Toolchain
enthalten. `loaderRange` ist optional und begrenzt die unterstützten Versionen
des Sacred Mod Loader. Beide Werte verwenden die Bereichsnotation von Maven.

`apiVersion` bezeichnet etwas anderes: die Artefaktversion von
`dev.ancaria.coderpack:api`, gegen die der Quelltext kompiliert wird. Sie ändert
sich bei Veröffentlichungen und ist derzeit `0.100.0`. Der API-Vertrag wird nur
bei inkompatiblen Änderungen hochgezählt.

Danach zwei Befehle:

```
gradlew assembleSacredMod
gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
```

Der erste Befehl schreibt `build/sacred-mod/my-mod-1.0.0.jar` und packt die
Abhängigkeiten des Mods mit ein. Der zweite kopiert diese Datei an das Ziel aus
`installTo`. Wer keine Property übergeben möchte, kann dort auch einen festen
Pfad eintragen:
`installTo = file("D:/SteamLibrary/steamapps/common/Sacred Gold/mods")`.

| Task | Was er tut |
|---|---|
| `generateModDescriptor` | schreibt `META-INF/declaration.toml` aus dem `sacred`-Block |
| `verifySacredMod` | prüft das gepackte JAR und schreibt `build/reports/sacred-mod/verify.txt` |
| `assembleSacredMod` | kopiert das geprüfte Fat JAR nach `build/sacred-mod` |
| `installSacredMod` | kopiert dieses JAR nach `installTo` |

Mods können in Java, Kotlin oder Groovy geschrieben werden.
`coderpack new --language kotlin` erzeugt das passende Projekt mit
Sprach-Plugin, der Laufzeitbibliothek unter `implementation` und einem
Bytecode-Ziel von Java 21. Dadurch landet die Laufzeitbibliothek im JAR, und die
Klassendateien funktionieren auf der JVM des Spielers. Das Packaging-Plugin
selbst wendet nur `java` an. Darauf bauen die übrigen JVM-Sprach-Plugins auf.

Die eingebettete Laufzeit wirkt sich auf die Dateigröße aus. Beim Template
`mod` ist ein Java-JAR etwa 1,8 KB groß, ein Kotlin-JAR 1,8 MB und ein
Groovy-JAR 7,8 MB. Jeder Mod läuft in einem eigenen Klassenlader, daher müssen
die Kotlin-Standardbibliothek beziehungsweise die Groovy-Laufzeit im JAR
liegen.

### Das JAR wird vor dem Kopieren geprüft

`assembleSacredMod` hängt von `verifySacredMod` ab. Erst nach einer erfolgreichen
Prüfung wird das gepackte JAR kopiert. Der Linter liest Deskriptor und Bytecode
auf dieselbe Weise wie der Loader und meldet unter anderem folgende Fehler:

- kein `META-INF/declaration.toml`, oder eines ohne `id`, `entrypoint` oder `api`
- ein ungültiger `api`-Bereich oder ein Bereich, der API-Vertrag 1 nicht enthält
- ein ungültiger `loader`-Bereich
- ein `entrypoint`, der nicht im JAR liegt, nicht `public` ist, abstrakt ist,
  keinen öffentlichen Konstruktor ohne Argumente hat oder `SacredMod` nicht
  implementiert
- die mitgepackte Loader-API
- eine `@Subscribe`-Methode, die nicht genau ein Event nimmt oder etwas
  zurückgibt
- Signaturdateien aus einer signierten Abhängigkeit, wegen denen der Klassenlader
  jede Klasse daneben ablehnt

Warnungen stoppen den Build nicht. Dazu gehören eine ungültige Mod-ID, eine
fehlende Version, ungültige oder selbstbezügliche Konflikte, mitgepackte
Zygote-Klassen, Klassendateien neuer als Java 21, eine aus dem JAR nicht
auflösbare Vererbung des Einstiegspunkts und nicht öffentliche Listener.

Ein Build, der darüber stolpert, hält so an:

```
> Task :verifySacredMod FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':verifySacredMod' (registered by plugin 'dev.ancaria.coderpack').
> my-mod-1.0.0.jar: 1 error
    error   listener    demo.MyMod.onHero takes 2 parameters. The bus registers a listener with exactly one, and the parameter is what it subscribes to
```

Derselbe Code läuft von der Kommandozeile, und so prüft ein Mod-Repository einen
Release, ohne ein Build-Werkzeug dazwischen:

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

Die Ausgabe enthält einen Block pro JAR. Bei mindestens einem Fehler endet der
Befehl mit Exit-Code 1, bei einem fehlerhaften Aufruf mit Exit-Code 2. Der
Linter lädt keine Klassen. ASM liest ausschließlich den Bytecode, sodass
statische Initialisierer aus fremden Mods nicht ausgeführt werden.

### Warum der Build so aufgebaut ist

**Der Deskriptor wird erzeugt.** Er enthält Angaben, die dem Build bereits
bekannt sind, vor allem die Version. So kann die Versionsnummer im Launcher
nicht von der im JAR abweichen.

**Ein JAR enthält alle Laufzeitabhängigkeiten.** Jeder Mod wird von seinem
eigenen Klassenlader geladen. Bibliotheken außerhalb des JARs sind für ihn
deshalb nicht sichtbar. Shadow übernimmt das Packen.

**Die API wird als `compileOnly` eingebunden.** Zygote stellt sie bereits auf
seinem Klassenpfad bereit. Eine zweite Kopie im Mod-JAR wäre für die JVM eine
andere Klasse mit demselben Namen und könnte eine `ClassCastException`
auslösen.

**Das Plugin wendet nur `java` an.** Die JVM-Sprach-Plugins bauen darauf auf.
Kotlin oder Groovy werden vom erzeugten Projekt selbst eingebunden, nicht vom
Packaging-Plugin.

## Was im Repository liegt

```
build/
  gradle/      das Gradle-Plugin, ein eigener Build
    plugin/    SacredPlugin, SacredExtension, Descriptor, Verify und der Funktionstest
  verify/      der Linter in Java, als Projekt in den Gradle-Build eingebunden
  templates/   coderpack: das Gerüst, die Kommandozeile des Linters,
               die Templates und die Sprachen
  maven/       eine Notiz zum Maven-Gegenstück, nach dem noch niemand gefragt hat
```

`verify/` liegt auf der obersten Ebene, weil die Gradle-Task nur einer von zwei
Aufrufern ist. Auch `templates/` gehört zu keinem einzelnen Build-System.
`gradle/settings.gradle.kts` bindet beide Verzeichnisse als `:verify` und
`:templates` ein. Deshalb baut `./gradlew build` alle drei Projekte.

Gemeinsame Regeln haben jeweils eine Quelle. `Ids.valid` legt die gültige
Schreibweise einer Mod-ID fest. Plugin und Scaffolder verwenden diese Methode,
bevor sie Dateien erzeugen. `Verifier.API` enthält den API-Vertrag, aus dem
`Verifier.API_RANGE` und damit der Standardwert von `Descriptor.API` abgeleitet
werden.

## Das Plugin bauen

```
cd gradle
./gradlew build
```

Der Build deklariert keine Java Toolchain. Kompiliert wird daher mit dem JDK,
unter dem Gradle läuft. Kotlin und Java erzeugen dennoch gezielt Bytecode für
Java 21. So liefern JDK 25 und das in CI eingesetzte Temurin 21 dieselben
Klassenversionen. Der Gradle Wrapper ist auf Gradle 9.7.1 festgelegt und prüft
beim Download die SHA-256-Prüfsumme.

Gruppe und Version des Plugins stehen in `gradle/gradle.properties` und nicht in
`plugin/build.gradle.kts`, denn aus dieser Datei liest die CI die Nummer, um zu
entscheiden, ob dieser Build veröffentlicht wird.

Die sieben Plugin-Tests verwenden Gradle TestKit und starten echte Builds.
Sie prüfen das Fat JAR, den erzeugten Deskriptor, Fehler bei Einstiegspunkt und
ID, API- und Loader-Bereiche sowie die Configuration Cache. Im gepackten JAR
müssen die Mod-Klasse und `org/jetbrains/annotations/NotNull.class` liegen,
während die Loader-API draußen bleibt.

Der End-to-End-Test des Scaffolders erzeugt mit `coderpack new` ein Projekt und
baut es anschließend mit einem zweiten Gradle-Prozess. Das Plugin wird dabei
aus Maven Local aufgelöst. Danach muss `Verifier.verify(jar).findings()` leer
sein, also auch frei von Warnungen. Der Ablauf wird für alle sechs Kombinationen
aus Sprache und Build-DSL wiederholt. Damit werden Java-21-Ziel, eingebettete
Sprachlaufzeit sowie die Gleichwertigkeit von `build.gradle` und
`build.gradle.kts` praktisch geprüft.

Schnellere Tests decken unter anderem ID-Regel, Platzhalter, Schreibschutz für
nicht leere Verzeichnisse, alle zwölf Template-Kombinationen,
`--no-registry`, `--template`, `--language`, `--dsl`, `--help` und
`coderpack templates` ab. Weil der End-to-End-Test Plugin und Linter aus Maven
Local auflöst, veröffentlicht `./gradlew build` die Projekte `:plugin` und
`:verify` dort.

Die Tests des Linters starten kein Gradle und laufen entsprechend schnell.
22 Fälle bauen jeweils ein echtes JAR aus kompilierten Fixture-Klassen. Jeder
Fall prüft eine konkrete Regel und stellt sicher, dass keine zusätzlichen Funde
auftauchen.

Der Build setzt `org.gradle.configuration-cache=true` und
`org.gradle.configuration-cache.problems=fail`. Der Funktionstest übergibt
außerdem `--configuration-cache`. Eine unzulässige `project`-Referenz in einer
Task-Aktion fällt dadurch in diesem Repository auf und nicht erst im Build
eines Mods.

### Veröffentlichen

```
./gradlew publishToMavenLocal
```

Für Mods auf demselben Rechner genügt dieser Befehl, weil erzeugte
Settings-Dateien `mavenLocal()` bereits enthalten. CI übernimmt die eigentliche
Veröffentlichung. Bei einem Push auf `master` liest sie `version` aus
`gradle/gradle.properties`. Fehlt der Tag `v<version>`, veröffentlicht sie die
Artefakte und legt anschließend den Tag an. Die Zugangsdaten kommen aus den
Gradle-Properties `gpr.user` und `gpr.key` oder aus `GITHUB_ACTOR` und
`GITHUB_TOKEN`.

Unter derselben Version werden die Implementierungsartefakte
`dev.ancaria.coderpack:plugin`, `dev.ancaria.coderpack:verify` und
`dev.ancaria.coderpack:templates` veröffentlicht. Gradle erzeugt außerdem den
Marker für das Plugin `dev.ancaria.coderpack`. Das Plugin benötigt den Linter
als Abhängigkeit, während das IntelliJ-IDEA-Plugin die Templates verwendet. Zum
GitHub Release gehört `coderpack-<version>.zip` mit Kommandozeilenprogramm und
Scaffolder.

## Mögliche Erweiterungen

- ein vollständiges Maven-Plugin unter `maven/`
- weitere Templates, etwa für einen Bibliotheks-Mod oder einen Mod mit eigener
  Konfigurationsdatei
- eine IDE-Startkonfiguration, die den Mod installiert und das Spiel startet

## Lizenz

Der Quelltext steht unter der MIT-Lizenz. Der vollständige Text befindet sich
in [LICENSE](LICENSE).
