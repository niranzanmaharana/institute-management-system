# Stops local IMS: API, gateway, Angular UI windows/processes, then Docker infra.
# Run from anywhere:  .\scripts\stop-dev.ps1
#
# Examples:
#   .\scripts\stop-dev.ps1
#   .\scripts\stop-dev.ps1 -SkipInfra     # leave Docker containers running
#   .\scripts\stop-dev.ps1 -Down          # docker compose down (remove containers)

[CmdletBinding()]
param(
  [switch] $SkipInfra,
  [switch] $Down
)

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$infra = Join-Path $root "infra"

$scriptPid = $PID
$script:killedPids = @{}
$walkableNames = @(
  "java", "javaw", "mvn", "cmd", "powershell", "pwsh", "node", "npm"
)
$boundaryNames = @(
  "explorer", "cursor", "code", "devenv", "wininit", "services", "lsass", "csrss", "smss", "conhost"
)

function Get-ParentProcessId {
  param([int] $ProcessId)
  try {
    $row = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop
    if ($null -ne $row.ParentProcessId) {
      return [int]$row.ParentProcessId
    }
  } catch {
  }
  return 0
}

function Get-TreeRootPid {
  param([int] $StartPid)
  $current = $StartPid
  $target = $StartPid
  $seen = @{}
  while ($current -gt 4 -and -not $seen.ContainsKey($current)) {
    $seen[$current] = $true
    if ($current -eq $scriptPid) {
      break
    }
    $proc = Get-Process -Id $current -ErrorAction SilentlyContinue
    if (-not $proc) {
      break
    }
    $name = $proc.ProcessName.ToLowerInvariant()
    if ($boundaryNames -contains $name) {
      break
    }
    if ($walkableNames -contains $name) {
      $target = $current
      $current = Get-ParentProcessId -ProcessId $current
      continue
    }
    break
  }
  if ($target -eq $scriptPid) {
    return $StartPid
  }
  return $target
}

function Stop-PidTree {
  param(
    [int] $ProcessId,
    [string] $Label
  )
  if ($ProcessId -le 4 -or $ProcessId -eq $scriptPid) {
    return
  }
  if ($script:killedPids.ContainsKey($ProcessId)) {
    return
  }
  $rootPid = Get-TreeRootPid -StartPid $ProcessId
  if ($script:killedPids.ContainsKey($rootPid)) {
    return
  }
  $proc = Get-Process -Id $rootPid -ErrorAction SilentlyContinue
  if (-not $proc) {
    $script:killedPids[$ProcessId] = $true
    $script:killedPids[$rootPid] = $true
    return
  }
  Write-Host "Stopping $Label ($($proc.ProcessName) PID $rootPid)..."
  cmd.exe /c "taskkill /PID $rootPid /T /F >nul 2>&1" | Out-Null
  $script:killedPids[$ProcessId] = $true
  $script:killedPids[$rootPid] = $true
}

function Get-PidsListeningOnPort {
  param([int] $Port)
  $pattern = ":$Port\s+\S+\s+LISTENING\s+(\d+)"
  $ids = @(
    netstat -ano -p tcp |
      Select-String -Pattern $pattern |
      ForEach-Object { [int]$_.Matches[0].Groups[1].Value } |
      Select-Object -Unique
  )
  return @($ids | Where-Object { $_ -and $_ -gt 4 })
}

function Stop-Port {
  param(
    [int] $Port,
    [string] $Name
  )
  $ids = Get-PidsListeningOnPort -Port $Port
  if ($ids.Count -eq 0) {
    Write-Host "$Name (:$Port) is not running."
    return
  }
  foreach ($id in $ids) {
    Stop-PidTree -ProcessId $id -Label "$Name :$Port"
  }
}

function Stop-Launchers {
  $hints = @(
    "IMS application :8080",
    "IMS gateway :8088",
    "IMS UI :4200",
    "ims-application spring-boot:run",
    "api-gateway spring-boot:run"
  )
  $filter = "Name = 'powershell.exe' OR Name = 'pwsh.exe'"
  $launchers = @(Get-CimInstance -ClassName Win32_Process -Filter $filter -ErrorAction SilentlyContinue | Where-Object {
    if (-not $_.CommandLine) { return $false }
    if ($_.ProcessId -eq $scriptPid) { return $false }
    foreach ($hint in $hints) {
      if ($_.CommandLine -like "*$hint*") { return $true }
    }
    $false
  })
  foreach ($launcher in $launchers) {
    Stop-PidTree -ProcessId ([int]$launcher.ProcessId) -Label "dev window"
  }
}

Write-Host "Repo: $root"
Write-Host "Stopping application processes..."

Stop-Launchers
Stop-Port -Port 4200 -Name "IMS UI"
Stop-Port -Port 8088 -Name "api-gateway"
Stop-Port -Port 8080 -Name "ims-application"

if (-not $SkipInfra) {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker is not on PATH; skipping infra."
  } else {
    $dockerOk = $true
    cmd.exe /c "docker info >nul 2>&1" | Out-Null
    if ($LASTEXITCODE -ne 0) {
      $dockerOk = $false
    }
    if (-not $dockerOk) {
      Write-Host "Docker engine is not running; skipping infra."
    } else {
      Push-Location $infra
      try {
        if ($Down) {
          Write-Host "Docker Compose down..."
          docker compose down
        } else {
          Write-Host "Stopping Compose services..."
          docker compose stop
        }
      } finally {
        Pop-Location
      }
    }
  }
}

Write-Host ""
Write-Host "Stopped."
Write-Host "  UI / gateway / API windows and port listeners were terminated."
if ($SkipInfra) {
  Write-Host "  Docker infra was left running (-SkipInfra)."
} elseif ($Down) {
  Write-Host "  Docker Compose containers were removed (-Down)."
} else {
  Write-Host "  Docker Compose services were stopped (containers kept)."
}
