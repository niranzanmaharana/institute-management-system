# Starts local IMS: optional Docker infra, then API, gateway, and Angular UI.
# Run from anywhere:  .\scripts\start-dev.ps1
#
# Defaults: MinIO only (local MySQL on 3306 is assumed). Jaeger/MySQL containers are skipped.
#
# Examples:
#   .\scripts\start-dev.ps1
#   .\scripts\start-dev.ps1 -FullInfra          # mysql + minio + jaeger
#   .\scripts\start-dev.ps1 -SkipInfra          # no Docker
#   .\scripts\start-dev.ps1 -SkipMavenInstall   # already built

[CmdletBinding()]
param(
  [switch] $FullInfra,
  [switch] $SkipInfra,
  [switch] $SkipMavenInstall
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$infra = Join-Path $root "infra"
$api = Join-Path $root "ims-api"
$ui = Join-Path $root "ims-ui"

function Wait-HttpOk {
  param(
    [string] $Url,
    [string] $Name,
    [int] $TimeoutSeconds = 180
  )
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  Write-Host "Waiting for $Name at $Url ..."
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
      if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
        Write-Host "$Name is up."
        return
      }
    } catch {
      Start-Sleep -Seconds 2
    }
  }
  throw "$Name did not become ready within ${TimeoutSeconds}s ($Url)"
}

function Start-DevWindow {
  param(
    [string] $Title,
    [string] $WorkingDirectory,
    [string] $Command
  )
  $escapedDir = $WorkingDirectory.Replace("'", "''")
  $inner = "Set-Location '$escapedDir'; Write-Host '=== $Title ==='; $Command"
  Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoExit",
    "-Command",
    $inner
  )
}

Write-Host "Repo: $root"

if (-not $SkipInfra) {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not on PATH. Install Docker Desktop or run with -SkipInfra."
  }
  try {
    docker info 2>$null | Out-Null
  } catch {
    throw "Docker engine is not running. Start Docker Desktop, wait until it is idle, then retry."
  }
  Push-Location $infra
  try {
    if ($FullInfra) {
      Write-Host "Starting Compose (mysql + minio + jaeger)..."
      docker compose up -d
    } else {
      Write-Host "Starting MinIO only (local MySQL on 3306; skip Jaeger)..."
      docker compose up -d minio
    }
  } finally {
    Pop-Location
  }
}

if (-not $SkipMavenInstall) {
  Write-Host "Maven install (skip tests)..."
  Push-Location $api
  try {
    mvn -pl ims-common,ims-application,api-gateway -am install "-DskipTests"
    if ($LASTEXITCODE -ne 0) {
      throw "Maven install failed with exit code $LASTEXITCODE"
    }
  } finally {
    Pop-Location
  }
}

Write-Host "Starting ims-application (:8080) in a new window..."
Start-DevWindow -Title "IMS application :8080" -WorkingDirectory $api -Command "mvn -pl ims-application spring-boot:run"
Wait-HttpOk -Url "http://localhost:8080/actuator/health" -Name "ims-application"

Write-Host "Starting api-gateway (:8088) in a new window..."
Start-DevWindow -Title "IMS gateway :8088" -WorkingDirectory $api -Command "mvn -pl api-gateway spring-boot:run"
Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Name "api-gateway"

if (-not (Test-Path (Join-Path $ui "node_modules"))) {
  Write-Host "Installing npm dependencies..."
  Push-Location $ui
  try {
    npm install
    if ($LASTEXITCODE -ne 0) {
      throw "npm install failed with exit code $LASTEXITCODE"
    }
  } finally {
    Pop-Location
  }
}

Write-Host "Starting Angular UI (:4200) in a new window..."
Start-DevWindow -Title "IMS UI :4200" -WorkingDirectory $ui -Command "npm start"

Write-Host ""
Write-Host "Started. Leave the new windows open."
Write-Host "  UI:      http://localhost:4200/login"
Write-Host "  Gateway: http://localhost:8088"
Write-Host "  API:     http://localhost:8080/swagger-ui.html"
Write-Host "  MinIO:   http://localhost:9001  (minio / minio12345)"
Write-Host "Demo: admin / Password@123 / DEMO_A"
