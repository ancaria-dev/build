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

Инструменты сборки модов для Sacred Gold: Gradle-плагин, линтер и утилита
`coderpack`, которая создаёт новые проекты.

Мод описывается одним блоком `sacred { }`. Плагин сам пишет дескриптор мода,
упаковывает код и библиотеки в один jar и проверяет его. Jar, который
загрузчик бы отверг, до папки игры не доберётся.

Здесь описана сборка. Сам API модов, события и слушатели описаны в
[coderpack](https://github.com/ancaria-dev/coderpack).

## Как начать

Нужен JDK. Gradle ставить не нужно, он приходит вместе с проектом.

1. Скачайте `coderpack-<версия>.zip` из
   [релизов](https://github.com/ancaria-dev/build/releases), распакуйте его и
   добавьте папку `bin` в PATH.
2. Создайте проект и соберите его:

   ```
   coderpack new my-mod
   cd my-mod
   gradlew assembleSacredMod
   ```

3. Скопируйте jar из `build/sacred-mod/` в `<Sacred Gold>/mods` или поручите
   это Gradle:

   ```
   gradlew installSacredMod -PsacredDir="D:\SteamLibrary\steamapps\common\Sacred Gold"
   ```

Удобнее в IDE? [Плагин для IntelliJ IDEA](https://github.com/ancaria-dev/idea)
создаёт тот же проект через File → New → Project.

## Новый проект

В команде `coderpack new my-mod` имя `my-mod` становится id мода. Утилита
проверяет его до того, как что-либо записать, и выводит из него отображаемое
имя `My Mod`, пакет `mods.mymod` и класс `MyMod`. В проекте появятся:

```
.gitignore
README.md
build.gradle.kts                     заполненный блок sacred { }
settings.gradle.kts                  сначала mavenLocal, затем портал плагинов
registry.toml                        описание репозитория модов
.github/workflows/build.yml          сборка и релиз каждой новой версии
dependencies.json                    версия coderpack для CI
gradlew, gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/mods/mymod/MyMod.java  onLoad и один рабочий слушатель
```

Wrapper совпадает с wrapper этого репозитория: Gradle 9.7.1 с той же
контрольной суммой SHA-256. Первая сборка запускается одной командой,
какой бы Gradle ни стоял в системе.

| Ключ | Что меняет |
|---|---|
| `--package dev.example.mymod` | Пакет и папку точки входа |
| `--display-name "My Splendid Mod"` | Имя, которое видит игрок |
| `--author "Your Name"` | Значение `authors`. По умолчанию — текущая учётная запись |
| `--description "One sentence"` | Строку под названием мода |
| `--mod-version 0.2.0` | Версию мода. По умолчанию `1.0.0` |
| `--dir somewhere/else` | Папку проекта. По умолчанию `./<name>` |
| `--template minimal` | Шаблон: `mod` (по умолчанию) или `minimal` |
| `--language kotlin` | Язык мода: `java` (по умолчанию), `kotlin` или `groovy` |
| `--dsl groovy` | Язык сборочного скрипта: `kotlin` (по умолчанию) или `groovy` |
| `--repo https://github.com/me/my-mod` | Адрес проекта для ссылок на скачивание |
| `--no-registry` | Не создаёт `registry.toml`, `dependencies.json` и workflow |
| `--git` | Выполняет `git init` и добавляет `--repo` как `origin` |
| `--force` | Пишет в непустую папку |

Любой язык сочетается с любым DSL. `coderpack templates` покажет, что есть, а
`--help` работает у каждой команды. Проект на Kotlin также получает
`dev.ancaria.coderpack:api-kotlin` — необязательные Kotlin-расширения, о них
рассказано в [coderpack](https://github.com/ancaria-dev/coderpack).

`--git` срабатывает после записи всех файлов. Если Git не установлен, проект
всё равно рабочий. Первый коммит утилита не делает.

## Блок sacred

`build.gradle.kts` описывает мод:

```kotlin
plugins {
    id("dev.ancaria.coderpack") version "0.200.0"
}

version = "1.0.0"

dependencies {
    // Всё, что нужно моду во время работы. Попадает внутрь jar.
    implementation("org.jetbrains:annotations:26.0.2")
}

sacred {
    id = "my-mod"                                     // строчные буквы, цифры, дефисы
    displayName = "My Mod"                            // что видит игрок
    description = "One sentence, shown under the name in the mod list"
    version = "1.0.0"                                 // по умолчанию версия проекта
    entrypoint = "demo.MyMod"                         // класс-наследник SacredMod
    authors = listOf("MairwunNx (Pavel Erokhin)")     // или author("…") по одному
    website = "https://ancaria.dev"
    repository = "https://github.com/ancaria-dev/mods"
    conflicts = listOf("other-mod")                   // или conflictsWith("other-mod")
    apiRange = "[3,4)"                                // поддерживаемые контракты API
    loaderRange = "[0.1.20,)"                         // необязательный диапазон версий загрузчика
    apiVersion = "0.200.0"                            // подключается как compileOnly
    installTo = layout.dir(providers.gradleProperty("sacredDir").map { file("$it/mods") })
}
```

Обязательны только `id` и `entrypoint`. `displayName` по умолчанию равен `id`,
а пустые необязательные значения в дескриптор не попадают.

Три настройки звучат похоже, но значат разное:

- `apiRange` — контракты API, с которыми работает мод, в нотации диапазонов
  Maven. `[3,4)` означает контракт 3 и всё до 4, не включая его. Диапазон
  можно сузить или расширить, но контракт 3, под который собирает этот
  инструментарий, должен в нём остаться.
- `loaderRange` ограничивает версии Sacred Mod Loader. Задавайте его, только
  если моду нужна конкретная версия. Плагин проверяет синтаксис, но не
  существование такой версии.
- `apiVersion` — Maven-версия `dev.ancaria.coderpack:api`. Плагин подключает её
  как `compileOnly`.

`installTo` можно задать и постоянным путём:
`installTo = file("D:/SteamLibrary/steamapps/common/Sacred Gold/mods")`.

| Задача | Что делает |
|---|---|
| `generateModDescriptor` | Пишет `META-INF/declaration.toml` из блока `sacred` |
| `verifySacredMod` | Проверяет собранный jar и пишет `build/reports/sacred-mod/verify.txt` |
| `assembleSacredMod` | Копирует проверенный jar в `build/sacred-mod` |
| `installSacredMod` | Копирует этот jar в `installTo` |

Всё, что подключено через `implementation`, оказывается внутри jar, в том
числе рантайм языка. Из шаблона `mod` jar на Java весит около 1,8 КБ, на
Kotlin — около 1,8 МБ, на Groovy — около 7,8 МБ. Все компиляторы собирают под
Java 21.

## Что проверяет линтер

`assembleSacredMod` сначала запускает `verifySacredMod`. Ошибка останавливает
сборку. Линтер считает ошибкой:

- нечитаемый jar или jar без `META-INF/declaration.toml`
- дескриптор без `id`, `entrypoint` или `api`
- нечитаемый диапазон API или загрузчика, а также диапазон API без контракта 3
- отсутствующую или абстрактную точку входа или точку входа, которая не
  наследует `SacredMod`
- точку входа, которая всё ещё реализует `SacredMod` как интерфейс из API 2
- точку входа без конструктора без аргументов
- классы API загрузчика, упакованные в мод
- метод с `@Subscribe`, у которого не ровно один параметр-событие, тип
  возврата не `void` и не `Mutation` этого же события, или метод `MONITOR`,
  который возвращает мутацию
- файлы подписи, скопированные из подписанной библиотеки

Предупреждения сборку не роняют. Они отмечают неверный id, отсутствие версии,
неверный конфликт или конфликт с самим собой, упакованные классы zygote,
class-файлы новее Java 21, точку входа с родительским классом вне jar, а также
непубличные точку входа, конструктор и слушатели.

Неудачная сборка выглядит так:

```
> Task :verifySacredMod FAILED
...
> my-mod-1.0.0.jar: 1 error
    error   listener    demo.MyMod.onHero takes 2 parameters. The bus registers a listener with exactly one, and the parameter is what it subscribes to
```

Те же проверки работают без Gradle. `coderpack verify` печатает отчёт по
каждому jar и завершается с кодом `1` при ошибке или `2` при неверных
аргументах:

```
coderpack verify build/sacred-mod/my-mod-1.0.0.jar
```

Линтер читает байткод через ASM и никогда не загружает ваши классы, поэтому
статические инициализаторы внутри сборки не выполняются.

## Публикация мода

Без `--no-registry` каждый новый проект сразу становится репозиторием модов,
на который можно подписаться в лаунчере:

```
coderpack new my-mod --repo https://github.com/me/my-mod --git
cd my-mod
gradlew assembleSacredMod
coderpack index
```

`registry.toml` вы ведёте сами:

```toml
name = "My Mod"
description = "My Mod, a mod for Sacred Gold"
url = "https://github.com/me/my-mod"
releases = "https://github.com/me/my-mod/releases/download/{id}-v{version}/{file}"
```

Обязательны `name`, `url` и `releases`. `description` и `icon` можно не
указывать. Шаблон `releases` собирает ссылку на скачивание и должен содержать
`{file}`. Без `--repo` утилита запишет `https://github.com/you/my-mod` и
подскажет, какую строку поправить.

`coderpack index` объединяет `registry.toml` с дескриптором каждого собранного
jar и пишет `sacred.mods.repository.json`. Не правьте этот файл руками. В нём
нет отметки времени, поэтому `coderpack index --check` сравнивает его побайтно.

Закоммитьте и запушьте. Готовый workflow соберёт мод, обновит и закоммитит
индекс, а затем создаст релиз `<id>-v<версия>`, если такого тега ещё нет. Пуш
без новой версии релиз не создаёт. Секреты не нужны, хватает собственного
токена GitHub.

## Почему сборка устроена так

**Дескриптор пишет плагин.** Версию и остальное сборка и так знает.
Сгенерированный дескриптор не разойдётся с jar.

**Библиотеки упаковываются в один jar.** У каждого мода свой загрузчик
классов, и он видит только содержимое jar. Упаковкой занимается Shadow.

**API подключается только для компиляции.** Загрузчик уже его предоставляет.
Вторая копия создаёт разные классы с одинаковыми именами и может закончиться
`ClassCastException`.

**Плагин применяет только `java`.** Плагины Kotlin и Groovy строятся поверх
него, поэтому язык мод выбирает и настраивает сам.

## Устройство репозитория

```
build/
  gradle/      сборка Gradle
    plugin/    SacredPlugin, SacredExtension, Descriptor, Verify и тесты
  verify/      линтер на Java
  templates/   coderpack, шаблоны проектов, языки и DSL
  maven/       заметки о возможном Maven-плагине, пока без реализации
```

Общие правила живут в `verify/`. `Ids.valid` определяет допустимый id мода,
и его вызывают и плагин, и утилита. `Verifier.API` хранит контракт API, а
`Verifier.API_RANGE` выводит из него диапазон по умолчанию.

Шаблоны, языки и DSL — это папки ресурсов в `templates/src/main/resources/`:
`templates/<name>/`, `languages/<name>/` и `dsl/<name>/`. Каждый файл лежит
там, где он меняется: точка входа зависит от шаблона и языка, сборочный скрипт
— от языка и DSL, файл настроек — только от DSL. Так 27 файлов дают 12
комбинаций. Сборка индексирует папки сама, поэтому новая папка не требует
кода на Kotlin.

Утилита подставляет `{{id}}`, `{{name}}`, `{{description}}`, `{{version}}`,
`{{package}}`, `{{packagePath}}`, `{{class}}`, `{{entrypoint}}`,
`{{author}}`, `{{repo}}`, `{{plugin}}` и `{{api}}`, а также все ключи, кроме
`description`, из `language.properties` и `dsl.properties`. Неразрешённый
плейсхолдер останавливает генерацию. Чтобы получить `.gitignore`, назовите
файл `_gitignore`: при копировании ресурсов файлы с точкой пропускаются.

Тот же модуль публикуется как `dev.ancaria.coderpack:templates`. Плагин для
IntelliJ IDEA вызывает его и не держит собственную копию шаблонов.

## Сборка

```
cd gradle
./gradlew build
```

Нужен JDK в PATH. CI использует Temurin 21. Блока toolchain нет: JDK, который
запускает Gradle, компилирует всё под Java 21. Кэш конфигурации включён и
падает на любой проблеме, поэтому задача, захватившая `project`, сломается
здесь, а не в чужом моде.

Тесты собирают настоящие проекты и настоящие jar. Тесты плагина используют
Gradle TestKit. Сквозной тест утилиты генерирует и собирает все шесть пар
языка и DSL и ждёт отчёт без единого предупреждения. Тесты линтера собирают
отдельный jar под каждое правило и не загружают ни одного класса.
`./gradlew build` также публикует плагин и линтер в Maven Local: сквозной тест
получает их так же, как сгенерированный мод.

Утилиту из исходников можно попробовать так:

```
./gradlew :templates:installDist :verify:demoModJar
../templates/build/install/coderpack/bin/coderpack verify ../verify/build/demo/demo-mod.jar
../templates/build/install/coderpack/bin/coderpack new demo-mod
```

Чтобы проверить невыпущенные изменения на моде на той же машине, выполните
`./gradlew publishToMavenLocal` здесь и в `coderpack`. Сгенерированные проекты
сначала смотрят в Maven Local.

## Релизы

Версия хранится в `gradle/gradle.properties`. `pwsh tools/version.ps1 0.200.1`
поднимает её вместе с `apiVersion` и всеми упоминаниями в README. Когда новая
версия попадает в `master`, CI публикует её в два места:

- линтер и утилита уходят в Maven Central одним подписанным архивом
- плагин уходит в Gradle Plugin Portal — только оттуда разрешается
  `id("dev.ancaria.coderpack")`

Затем CI создаёт тег `v<версия>` и прикладывает `coderpack-<версия>.zip` к
GitHub-релизу. У `plugin`, `verify` и `templates` всегда одна версия: плагин
зависит от линтера, а плагин IDE — от шаблонов.

Для Central нужны `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY` и
`SIGNING_PASSWORD`, для портала — `GRADLE_PUBLISH_KEY` и
`GRADLE_PUBLISH_SECRET`. Загрузка в Central ждёт, пока кто-нибудь нажмёт
Publish в портале. Порядок релизов всего загрузчика описан в
[CONTRIBUTING](https://github.com/ancaria-dev/.github/blob/master/CONTRIBUTING.md).

## Лицензия

MIT, см. [LICENSE](LICENSE).
