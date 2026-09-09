<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Shadow](https://img.shields.io/badge/Shadow-9.6.1-1E88E5?style=for-the-badge)](https://gradleup.com/shadow/)
[![License](https://img.shields.io/badge/License-MIT-4B5563?style=for-the-badge)](LICENSE)
[![Sacred](https://img.shields.io/badge/Sacred-Community-8B1A1A?style=for-the-badge&labelColor=1C1410)](https://ancaria.dev)

[English](README.EN.md) · [Deutsch](README.DE.md)

</div>

# build

Инструменты сборки модов для Sacred Gold.

В `gradle/` находится плагин `dev.ancaria.coderpack`. Он создаёт дескриптор
мода, собирает fat jar через Shadow, проверяет результат и устанавливает его в
каталог игры. В `maven/` пока лежит только описание возможного Maven-плагина.

Каталог `verify/` содержит Java-библиотеку, которая проверяет готовый jar без
загрузки классов. Её вызывают задача Gradle `verifySacredMod` и команда
`coderpack verify`.

В `templates/` собирается командная строка `coderpack`. Она создаёт проекты,
проверяет jar-файлы и генерирует индекс репозитория модов. Тот же модуль
публикуется как библиотека `dev.ancaria.coderpack:templates` для плагина
IntelliJ IDEA.

## Создание мода

```
coderpack new my-mod
cd my-mod
gradlew assembleSacredMod
```

Последняя команда создаёт проверенный jar в `build/sacred-mod/`. Для сборки
нужен JDK. Gradle устанавливать отдельно не требуется, поскольку новый проект
получает wrapper.

Архив `coderpack` прикладывается к каждому релизу этого репозитория. Из
исходников локальную копию можно собрать так:

```
cd gradle
./gradlew :templates:installDist
../templates/build/install/coderpack/bin/coderpack new my-mod
```

Проект `my-mod/` по умолчанию содержит:

```
.gitignore
README.md
build.gradle.kts                     заполненный блок sacred { }
settings.gradle.kts                  mavenLocal, затем портал плагинов
registry.toml                        описание репозитория модов
.github/workflows/build.yml          сборка, проверка индекса и релиз
dependencies.json                    версия coderpack, которую скачивает CI
gradlew, gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/mods/mymod/MyMod.java  onLoad и рабочий слушатель
```

По умолчанию исходники пишутся на Java, а файлы сборки используют Kotlin DSL.
`--language kotlin` создаёт файл
`src/main/kotlin/mods/mymod/MyMod.kt` и подключает Kotlin. Значение
`--language groovy` выбирает Groovy. Опция `--dsl groovy` создаёт
`build.gradle` и `settings.gradle` независимо от языка мода.

`--no-registry` исключает `registry.toml`, `dependencies.json` и
`.github/workflows/build.yml`. Остальная часть проекта не меняется.

`my-mod` служит идентификатором мода. Перед записью файлов он проверяется через
`Ids.valid`. Из него выводятся отображаемое имя `My Mod`, пакет `mods.mymod` и
класс `MyMod`.

| Опция | Назначение |
|---|---|
| `--package dev.example.mymod` | Задаёт пакет и каталог точки входа |
| `--display-name "Мой мод"` | Задаёт имя в лаунчере |
| `--author "Ваше имя"` | Задаёт `authors`, иначе используется имя текущей учётной записи |
| `--description "Одно предложение"` | Задаёт описание в списке модов |
| `--mod-version 0.2.0` | Задаёт версию мода, по умолчанию `1.0.0` |
| `--dir куда-нибудь/ещё` | Задаёт каталог, по умолчанию `./<name>` |
| `--template minimal` | Выбирает шаблон |
| `--language kotlin` | Выбирает `java`, `kotlin` или `groovy` |
| `--dsl groovy` | Выбирает `kotlin` или `groovy` для файлов сборки |
| `--repo https://github.com/me/my-mod` | Задаёт адрес репозитория и основу ссылок на релизы |
| `--no-registry` | Не создаёт файлы репозитория модов |
| `--git` | Выполняет `git init` и добавляет `--repo` как `origin` |
| `--force` | Разрешает запись в непустой каталог |

Скаффолдер переносит в проект wrapper из этой сборки. Сейчас он закреплён на
Gradle 9.7.1, а дистрибутив проверяется по SHA-256 из
`gradle/wrapper/gradle-wrapper.properties`.

### Публикация мода

Новый проект по умолчанию является репозиторием модов в формате SRML:

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
gradlew assembleSacredMod
coderpack index
```

`coderpack index` читает `registry.toml` и дескриптор внутри собранного jar,
после чего пишет `sacred.mods.repository.json`. В индекс попадают версия,
диапазоны API и загрузчика, авторы, конфликты, имя файла, размер, SHA-256 и
ссылка на скачивание. При наличии добавляются сайт, путь к исходникам и иконка.
В корне JSON поле `srml` равно `1`, рядом находятся имя, описание, URL, иконка
и массив модов. Временной метки нет, поэтому `coderpack index --check` может
сравнивать результат побайтно.

Закоммитьте `registry.toml` и `sacred.mods.repository.json`. Сгенерированный
workflow собирает мод, проверяет индекс и создаёт релиз с тегом
`<id>-v<version>`, если такого тега ещё нет. Он использует
`${{ github.token }}` и не требует отдельного секрета. Для скачивания
`coderpack` workflow обращается к последнему релизу `ancaria-dev/build`, поэтому
эта часть CI начнёт работать после первого релиза инструментов.

В `registry.toml` четыре поля:

```toml
name = "My Mod"
description = "My Mod, a mod for Sacred Gold"
url = "https://github.com/me/my-mod"
releases = "https://github.com/me/my-mod/releases/download/{id}-v{version}/{file}"
```

Обязательны `name`, `url` и `releases`, а `description` и `icon` необязательны.
Шаблон `releases` должен содержать `{file}`. Если `--repo` не указан,
скаффолдер записывает заметный шаблон `https://github.com/you/my-mod` и
сообщает, что `url` нужно исправить.

Опция `--git` запускается после записи проекта. Ошибка Git не удаляет готовые
файлы. Коммит скаффолдер не создаёт.

### Шаблоны, языки и DSL

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

Шаблоны находятся в
`templates/src/main/resources/templates/<name>/`. Каталог `files/` содержит
общие файлы шаблона, а `lang/<language>/` содержит точку входа для конкретного
языка.

Языки находятся в `templates/src/main/resources/languages/<name>/`.
`language.properties` задаёт описание, каталог исходников, расширение и версию
компилятора. Файл сборки для каждой пары языка и DSL лежит в
`dsl/<dsl>/`.

Описания DSL находятся в `templates/src/main/resources/dsl/<name>/`.
`dsl.properties` задаёт имена build-файла и settings-файла, а `files/`
содержит settings-файл.

Общие файлы проекта лежат в `common/files/`. Файлы SRML находятся в
`repository/files/` и добавляются, если не указан `--no-registry`.

В содержимом и именах файлов заменяются `{{id}}`, `{{name}}`,
`{{description}}`, `{{version}}`, `{{package}}`, `{{packagePath}}`,
`{{class}}`, `{{entrypoint}}`, `{{author}}`, `{{repo}}`, `{{plugin}}` и
`{{api}}`. К ним добавляются ключи из `language.properties` и
`dsl.properties`, кроме `description`. Неизвестный плейсхолдер останавливает
создание проекта до записи первого файла.

Задача `resourceIndex` индексирует ресурсы во время сборки. Поэтому новый
шаблон, язык или DSL добавляется файлами, без списка имён в Kotlin. Имя
`_gitignore` превращается в `.gitignore`, поскольку копирование ресурсов и
индексатор пропускают dot-файлы.

Несуществующий язык или DSL отклоняется до записи проекта:

```
coderpack: there is no language "rust". There is: groovy, java, kotlin
```

`--help` и `-h` работают у каждой подкоманды. `coderpack new --help` выводит
опции команды `new`, а `coderpack help` добавляет общий список команд.

### Проверка jar

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

Команда принимает несколько путей и печатает отдельный отчёт для каждого jar.
Код возврата равен 1, если найдена ошибка, и 2 при неправильном вызове.
Предупреждения не меняют код возврата.

## Настройка сборки мода

`coderpack new` создаёт settings-файл со следующими репозиториями:

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

`mavenLocal()` указан первым. До публикации инструментов выполните
`./gradlew publishToMavenLocal` в репозиториях `build` и `coderpack`. Первый
публикует плагин, линтер и шаблоны. Второй публикует API загрузчика.

Пример `build.gradle.kts` со всеми основными свойствами:

```kotlin
plugins {
    id("dev.ancaria.coderpack") version "0.99.0"
}

version = "1.0.0"

dependencies {
    // Зависимости времени выполнения попадут внутрь jar.
    implementation("org.jetbrains:annotations:26.0.2")
}

sacred {
    id = "my-mod"
    displayName = "My Mod"
    description = "Одно предложение под названием в списке модов"
    version = "1.0.0"
    entrypoint = "demo.MyMod"
    authors = listOf("MairwunNx (Pavel Erokhin)")
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    conflicts = listOf("another-mod")
    apiRange = "[1,2)"
    loaderRange = "[0.1.20,)"
    apiVersion = "0.99.0"
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
```

Обязательны только `id` и `entrypoint`. `displayName` по умолчанию совпадает с
`id`, а `version` берётся из версии проекта. `authors` и `conflicts` можно
пополнять методами `author(name)` и `conflictsWith(id)`.

`apiRange` описывает совместимые контракты API в синтаксисе диапазонов Maven.
Значение по умолчанию сейчас равно `"[1,2)"`. Плагин не разрешает диапазон, в
который не входит контракт `Verifier.API`, сейчас это `"1"`.

`loaderRange` задаёт совместимые версии Sacred Mod Loader. Оно необязательно.
Плагин и линтер проверяют его синтаксис, но не существование релизов.

`apiVersion` имеет другой смысл. Это версия артефакта
`dev.ancaria.coderpack:api`, который добавляется как `compileOnly`. Сейчас
скаффолдер записывает `"0.99.0"`.

Сборка и установка выполняются так:

```
gradlew assembleSacredMod
gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
```

Первая команда создаёт
`build/sacred-mod/my-mod-1.0.0.jar` с зависимостями времени выполнения. Вторая
копирует его в каталог `installTo`. Путь можно задать непосредственно:
`installTo = file("D:/SteamLibrary/steamapps/common/Sacred Gold/mods")`.

| Задача | Результат |
|---|---|
| `generateModDescriptor` | Пишет `META-INF/declaration.toml` из блока `sacred` |
| `verifySacredMod` | Проверяет результат `shadowJar` и пишет `build/reports/sacred-mod/verify.txt` |
| `assembleSacredMod` | Копирует проверенный fat jar в `build/sacred-mod` |
| `installSacredMod` | Копирует собранный jar в `installTo` |

Плагин применяет `java` и `com.gradleup.shadow`. Kotlin- и Groovy-плагины
добавляются сгенерированными build-файлами. Для Java используется
`options.release = 21`. Kotlin получает `jvmTarget = JVM_21`, а Groovy
получает `sourceCompatibility`, `targetCompatibility` и `options.release`,
равные 21.

Стандартная библиотека Kotlin и среда выполнения Groovy объявлены через
`implementation`, чтобы Shadow поместил их в jar. Измерения для шаблона `mod`
дают примерно 1,8 КБ для Java, 1,8 МБ для Kotlin и 7,8 МБ для Groovy.

### Что проверяет линтер

`assembleSacredMod` зависит от `verifySacredMod`, поэтому непрошедший проверку
jar не копируется в выходной каталог и в папку игры.

Ошибками считаются:

- нечитаемый jar или отсутствие `META-INF/declaration.toml`
- отсутствие `id`, `entrypoint` или `api`
- неверный диапазон `api`, диапазон без `Verifier.API` или неверный
  `loaderRange`
- отсутствующий, непубличный или абстрактный `entrypoint`
- отсутствие публичного конструктора без аргументов
- точка входа, которая не реализует `SacredMod`
- классы `dev/ancaria/coderpack/api/` внутри jar
- файлы подписи `META-INF/*.SF`, `*.DSA` или `*.RSA`
- метод `@Subscribe` с неверным числом параметров, не-событием или
  возвращаемым значением

Предупреждения сообщают о некорректном `id`, отсутствующей версии, неверных
`conflicts`, классах зиготы, class-файлах новее Java 21, непубличных
слушателях и случаях, когда внешняя иерархия классов не позволяет доказать
реализацию `SacredMod`.

Сокращённый пример ошибки Gradle:

```
> Task :verifySacredMod FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':verifySacredMod'.
> my-mod-1.0.0.jar: 1 error
    error   listener    demo.MyMod.onHero takes 2 parameters
```

Ту же проверку можно запустить напрямую:

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

Линтер читает class-файлы через ASM с `ClassReader.SKIP_CODE`. Классы мода не
загружаются, поэтому их статические инициализаторы не выполняются.

### Почему сборка устроена так

Дескриптор генерируется из блока `sacred`, чтобы версия и точка входа
соответствовали собранному jar.

Shadow упаковывает зависимости времени выполнения. Каждый мод загружается
собственным загрузчиком классов, поэтому нужная библиотека должна находиться
внутри jar.

API загрузчика остаётся `compileOnly`. Его уже предоставляет загрузчик, а
вторая копия тех же классов внутри мода вызвала бы несовместимость типов.

Плагин сборки не выбирает язык мода. Он применяет только базовый Java-плагин, а
Kotlin или Groovy подключаются в проекте мода.

## Структура репозитория

```
build/
  gradle/      отдельная Gradle-сборка
    plugin/    SacredPlugin, SacredExtension, Descriptor, Verify и тесты
  verify/      линтер на Java
  templates/   coderpack, шаблоны, языки и DSL
  maven/       описание возможного Maven-плагина
```

`gradle/settings.gradle.kts` подключает `verify/` как проект `:verify`, а
`templates/` как `:templates`. Команда `./gradlew build` из каталога
`gradle/` собирает и тестирует все три модуля.

Правило идентификатора находится в `Ids.valid`. Номер контракта хранится в
`Verifier.API`, а диапазон по умолчанию вычисляется как `Verifier.API_RANGE`.
Плагин и скаффолдер используют эти значения из модуля `verify`.

## Сборка и тесты

```
cd gradle
./gradlew build
```

Сборка не объявляет Java toolchain. Её запускает установленный JDK, а
`options.release` и `jvmTarget` закрепляют выходные class-файлы на Java 21.
CI использует Temurin 21.

Функциональные тесты плагина запускают вторую сборку через Gradle TestKit,
создают настоящий mod jar и проверяют дескриптор, класс точки входа, fat jar,
ошибочный идентификатор и отсутствующую точку входа.

End-to-end тесты скаффолдера создают и собирают проекты для шести сочетаний
языка и DSL. Готовые jar должны пройти `Verifier.verify` без ошибок и
предупреждений. Быстрые тесты отдельно проверяют имена, плейсхолдеры,
непустые каталоги, параметры команд и состав файлов.

Тесты линтера собирают jar-файлы из фикстурных классов. Каждый случай проверяет
конкретное правило и отсутствие лишних находок.

В `gradle/gradle.properties` включены:

```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=fail
```

Функциональные тесты также запускаются с `--configuration-cache`.

### Публикация инструментов

```
./gradlew publishToMavenLocal
```

Команда публикует в Maven Local плагин, линтер и библиотеку шаблонов.

CI читает `version` из `gradle/gradle.properties`. При пуше в `master` и
отсутствии тега `v<version>` он публикует в два места, потому что две половины
этого репозитория потребляются по-разному: линтер и шаблоны уходят одним
подписанным архивом в Maven Central, а плагин — в Gradle Plugin Portal, откуда
и только откуда резолвится `id("dev.ancaria.coderpack")` в сборке мода. Затем
создаётся GitHub Release.

Для Central нужны `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY` и
`SIGNING_PASSWORD`, для портала — `GRADLE_PUBLISH_KEY` и
`GRADLE_PUBLISH_SECRET`. Без ключа подписи задачи подписи пропускаются, а не
падают. Загрузка в Central ждёт в портале нажатия Publish: удалить оттуда
артефакт нельзя никогда.

Под одной версией публикуются артефакты
`dev.ancaria.coderpack:plugin`, `dev.ancaria.coderpack:verify` и
`dev.ancaria.coderpack:templates`, а также маркер плагина
`dev.ancaria.coderpack`. К GitHub Release прикладывается
`coderpack-<version>.zip`.

## Возможные дополнения

- Maven-плагин в `maven/`
- новые шаблоны и языки
- новые варианты DSL

## Лицензия

Проект распространяется по лицензии MIT. Полный текст находится в
[LICENSE](LICENSE).
