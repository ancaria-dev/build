<#
.SYNOPSIS
    Prints or sets the version of the plugin, verify, and templates, everywhere it is written.

.DESCRIPTION
    gradle/gradle.properties carries two numbers that happen to be equal right
    now: version, this repository's own release, and apiVersion, the default
    dev.ancaria.coderpack:api version a generated project pins. They track two
    different repositories and can drift apart the day coderpack releases
    without a matching build release. This script bumps both together because
    that is the common case; if they ever need to diverge, edit
    gradle.properties by hand afterward.

    The Javadoc in Descriptor.kt and the three READMEs repeat one or both
    numbers for a human to read, so they move too.

.EXAMPLE
    pwsh tools/version.ps1
    pwsh tools/version.ps1 0.99.1
#>
param(
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$propsPath = Join-Path $root 'gradle/gradle.properties'

$match = Select-String -Path $propsPath -Pattern '^version=(.+)$'
if (-not $match) { throw "No version= line in $propsPath" }
$current = $match.Matches[0].Groups[1].Value

if (-not $Version) {
    Write-Host $current
    return
}

$targets = @(
    (Join-Path $root 'gradle/gradle.properties'),
    (Join-Path $root 'gradle/plugin/src/main/kotlin/dev/ancaria/coderpack/plugin/Descriptor.kt'),
    (Join-Path $root 'README.md'),
    (Join-Path $root 'README.EN.md'),
    (Join-Path $root 'README.DE.md')
)

$pattern = "(?<!\d)$([regex]::Escape($current))(?!\d)"
$touched = 0
foreach ($path in $targets) {
    $text = Get-Content -Path $path -Raw
    $new = [regex]::Replace($text, $pattern, $Version)
    if ($new -eq $text) {
        Write-Warning "$current not found in $path, left untouched"
        continue
    }
    Set-Content -Path $path -Value $new -NoNewline
    $touched++
}

Write-Host "$current -> $Version in $touched file(s)"
