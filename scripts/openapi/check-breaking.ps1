# Compare two OpenAPI specs for breaking changes (oasdiff via Docker).
# Fails when breaking changes exist without a semver major bump (minor while <1.0.0).
#
#   .\scripts\openapi\check-breaking.ps1 -Base docs/api/openapi.json -Revision path\to\revised.json
#   .\scripts\openapi\check-breaking.ps1 -Base docs/api/openapi.json -Revision docs/api/openapi.json

param(
  [Parameter(Mandatory = $true)][string]$Base,
  [Parameter(Mandatory = $true)][string]$Revision,
  [ValidateSet("MISSING-VERSION-BUMP", "ERR", "WARN")]
  [string]$FailOn = "MISSING-VERSION-BUMP"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Resolve-Spec([string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) { return (Resolve-Path $Path).Path }
  return (Resolve-Path (Join-Path $root $Path)).Path
}

$basePath = Resolve-Spec $Base
$revPath = Resolve-Spec $Revision
$baseName = Split-Path $basePath -Leaf
$revName = Split-Path $revPath -Leaf
$baseDir = Split-Path $basePath -Parent
$revDir = Split-Path $revPath -Parent

Write-Host "oasdiff breaking ($FailOn)"
Write-Host "  base:     $basePath"
Write-Host "  revision: $revPath"

# Mount each parent; if same dir, one mount is enough.
if ($baseDir -eq $revDir) {
  $mount = $baseDir -replace '\\', '/'
  docker run --rm -v "${mount}:/specs" tufin/oasdiff:latest breaking `
    --fail-on $FailOn "/specs/$baseName" "/specs/$revName"
} else {
  $bMount = $baseDir -replace '\\', '/'
  $rMount = $revDir -replace '\\', '/'
  docker run --rm `
    -v "${bMount}:/base" `
    -v "${rMount}:/rev" `
    tufin/oasdiff:latest breaking `
    --fail-on $FailOn "/base/$baseName" "/rev/$revName"
}

exit $LASTEXITCODE
