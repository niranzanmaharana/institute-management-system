# Step 7.3 host-side p95 smoke (no Docker). Useful when k6→host networking is flaky.
#
#   .\scripts\perf\run-powershell-baseline.ps1

[CmdletBinding()]
param(
  [string] $BaseUrl = "http://127.0.0.1:8080",
  [string] $Username = "admin-a",
  [string] $Password = "Password@123",
  [string] $Institute = "DEMO_A",
  [int] $LoginSamples = 40,
  [int] $ReadSamples = 60
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$outJson = Join-Path $root "docs\perf\powershell-baseline-summary.json"

function Measure-P95([double[]] $samples) {
  $sorted = $samples | Sort-Object
  if ($sorted.Count -eq 0) { return 0 }
  $idx = [math]::Max(0, [math]::Ceiling(0.95 * $sorted.Count) - 1)
  return [math]::Round($sorted[$idx], 2)
}

function Invoke-TimedLogin {
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  $body = @{ username = $Username; password = $Password; instituteCode = $Institute } | ConvertTo-Json
  $resp = Invoke-WebRequest -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json" -UseBasicParsing
  $sw.Stop()
  if ($resp.StatusCode -ne 200) { throw "login HTTP $($resp.StatusCode)" }
  $token = ($resp.Content | ConvertFrom-Json).accessToken
  return @{ Ms = $sw.Elapsed.TotalMilliseconds; Token = $token }
}

function Invoke-TimedGet([string] $path, [string] $token) {
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  $resp = Invoke-WebRequest -Uri "$BaseUrl$path" -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing
  $sw.Stop()
  if ($resp.StatusCode -ne 200) { throw "GET $path HTTP $($resp.StatusCode)" }
  return $sw.Elapsed.TotalMilliseconds
}

Write-Host "Warmup login..."
$warm = Invoke-TimedLogin
$token = $warm.Token

$loginMs = New-Object System.Collections.Generic.List[double]
$outstandingMs = New-Object System.Collections.Generic.List[double]
$searchMs = New-Object System.Collections.Generic.List[double]

Write-Host "Login samples: $LoginSamples"
for ($i = 0; $i -lt $LoginSamples; $i++) {
  $loginMs.Add((Invoke-TimedLogin).Ms)
}

Write-Host "Read samples: $ReadSamples (outstanding + student search)"
for ($i = 0; $i -lt $ReadSamples; $i++) {
  $outstandingMs.Add((Invoke-TimedGet "/api/v1/outstanding" $token))
  $searchMs.Add((Invoke-TimedGet "/api/v1/students?q=a&status=ACTIVE&page=0&size=20" $token))
}

$summary = [ordered]@{
  capturedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
  baseUrl       = $BaseUrl
  username      = $Username
  institute     = $Institute
  login = @{
    n   = $loginMs.Count
    avg = [math]::Round((($loginMs | Measure-Object -Average).Average), 2)
    p95 = Measure-P95 $loginMs.ToArray()
    max = [math]::Round((($loginMs | Measure-Object -Maximum).Maximum), 2)
  }
  outstanding = @{
    n   = $outstandingMs.Count
    avg = [math]::Round((($outstandingMs | Measure-Object -Average).Average), 2)
    p95 = Measure-P95 $outstandingMs.ToArray()
    max = [math]::Round((($outstandingMs | Measure-Object -Maximum).Maximum), 2)
  }
  studentSearch = @{
    n   = $searchMs.Count
    avg = [math]::Round((($searchMs | Measure-Object -Average).Average), 2)
    p95 = Measure-P95 $searchMs.ToArray()
    max = [math]::Round((($searchMs | Measure-Object -Maximum).Maximum), 2)
  }
  targetsMs = @{ loginP95 = 800; outstandingP95 = 500; studentSearchP95 = 500 }
}

$summary | ConvertTo-Json -Depth 5 | Set-Content -Path $outJson -Encoding UTF8
Write-Host ($summary | ConvertTo-Json -Depth 5)
Write-Host "Wrote $outJson"
