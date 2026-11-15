# Step 7.3 — run k6 smoke via Docker and print summary JSON path.
#
# Prerequisites: API (+ gateway) healthy; Docker Desktop; grafana/k6 image.
#
#   .\scripts\perf\run-k6-baseline.ps1
#   .\scripts\perf\run-k6-baseline.ps1 -BaseUrl http://host.docker.internal:8080

[CmdletBinding()]
param(
  # Hit API directly by default — gateway login rate-limit skews concurrent login smoke.
  [string] $BaseUrl = "http://host.docker.internal:8080",
  [string] $Username = "admin-a",
  [string] $Password = "Password@123",
  [string] $Institute = "DEMO_A"
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $false
}

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$perfDir = Join-Path $root "docs\perf"
$script = Join-Path $perfDir "k6-smoke.js"
$outJson = Join-Path $perfDir "k6-baseline-summary.json"
$outTxt = Join-Path $perfDir "k6-baseline-console.txt"

if (-not (Test-Path $script)) {
  throw "Missing $script"
}

$mount = (Resolve-Path $perfDir).Path -replace '\\', '/'
if ($mount -match '^([A-Za-z]):/(.*)$') {
  $mount = "/$($Matches[1].ToLower())/$($Matches[2])"
}

Write-Host "Running k6 against $BaseUrl ..."
$dockerArgs = @(
  "run", "--rm",
  "-e", "BASE_URL=$BaseUrl",
  "-e", "USERNAME=$Username",
  "-e", "PASSWORD=$Password",
  "-e", "INSTITUTE=$Institute",
  "-v", "${mount}:/scripts",
  "grafana/k6:latest",
  "run", "--summary-export=/scripts/k6-baseline-summary.json", "/scripts/k6-smoke.js"
)
$console = & docker @dockerArgs 2>&1 | Out-String
Set-Content -Path $outTxt -Value $console -Encoding UTF8
Write-Host $console

Write-Host "Summary JSON: $outJson"
Write-Host "Console log:  $outTxt"
if (-not (Test-Path $outJson)) {
  throw "k6 did not write summary export"
}
# k6 exits non-zero when thresholds fail — surface that clearly
if ($LASTEXITCODE -ne 0) {
  Write-Warning "k6 exited with code $LASTEXITCODE (see thresholds in console log)"
  exit $LASTEXITCODE
}
