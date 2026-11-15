# Restore a mysqldump into a disposable scratch MySQL on host port 3308.
# Does NOT touch the live IMS database.
#
# Examples:
#   .\scripts\backup\restore-mysql-scratch.ps1 -DumpFile backups\mysql\ims-....sql
#   .\scripts\backup\restore-mysql-scratch.ps1 -DumpFile ... -KeepContainer

[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string] $DumpFile,
  [int] $ScratchPort = 3308,
  [string] $ContainerName = "ims-mysql-scratch",
  [switch] $KeepContainer
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $false
}
if (-not (Test-Path $DumpFile)) {
  throw "Dump file not found: $DumpFile"
}

$mysql = "C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe"
if (-not (Test-Path $mysql)) {
  $cmd = Get-Command mysql -ErrorAction SilentlyContinue
  if ($cmd) { $mysql = $cmd.Source } else { $mysql = $null }
}
if (-not $mysql) {
  throw "mysql client not found."
}

Write-Host "Removing previous scratch container (if any)..."
cmd.exe /c "docker rm -f $ContainerName >nul 2>&1" | Out-Null

Write-Host "Starting scratch MySQL on port $ScratchPort..."
docker run -d `
  --name $ContainerName `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=ims `
  -p "${ScratchPort}:3306" `
  mysql:8.4 | Out-Null

$deadline = (Get-Date).AddSeconds(120)
do {
  Start-Sleep -Seconds 3
  cmd.exe /c "docker exec $ContainerName mysqladmin ping -h 127.0.0.1 -uroot -proot --silent >nul 2>&1" | Out-Null
  if ($LASTEXITCODE -eq 0) { break }
  if ((Get-Date) -gt $deadline) { throw "Scratch MySQL did not become ready in time." }
} while ($true)

Write-Host "Restoring $DumpFile into $ContainerName..."
$dumpPath = (Resolve-Path $DumpFile).Path
$env:MYSQL_PWD = "root"
try {
  $restoreCmd = "`"$mysql`" --host=127.0.0.1 --port=$ScratchPort --user=root < `"$dumpPath`""
  cmd.exe /c $restoreCmd
  if ($LASTEXITCODE -ne 0) {
    throw "mysql restore failed with exit $LASTEXITCODE"
  }

  Write-Host "Verifying restored schema..."
  $tables = & $mysql --host=127.0.0.1 --port=$ScratchPort --user=root -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ims';"
  $students = & $mysql --host=127.0.0.1 --port=$ScratchPort --user=root -N -e "SELECT COUNT(*) FROM ims.students;"
  $institutes = & $mysql --host=127.0.0.1 --port=$ScratchPort --user=root -N -e "SELECT COUNT(*) FROM ims.institutes;"
  $flyway = & $mysql --host=127.0.0.1 --port=$ScratchPort --user=root -N -e "SELECT COUNT(*) FROM ims.flyway_schema_history;"
} finally {
  Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

$result = [pscustomobject]@{
  DumpFile          = (Resolve-Path $DumpFile).Path
  ScratchPort       = $ScratchPort
  ContainerName     = $ContainerName
  TableCount        = [int]("$tables".Trim())
  InstituteCount    = if ($institutes) { [int]("$institutes".Trim()) } else { -1 }
  StudentCount      = if ($students) { [int]("$students".Trim()) } else { -1 }
  FlywayHistoryRows = if ($flyway) { [int]("$flyway".Trim()) } else { -1 }
  RestoredAtUtc     = (Get-Date).ToUniversalTime().ToString("o")
}

if ($result.TableCount -lt 1) {
  throw "Restore verification failed: no tables in ims schema."
}

Write-Host ($result | Format-List | Out-String)

if (-not $KeepContainer) {
  Write-Host "Removing scratch container $ContainerName..."
  docker rm -f $ContainerName | Out-Null
}

$result
