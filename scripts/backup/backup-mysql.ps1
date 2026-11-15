# Backup IMS MySQL database to backups/mysql/
# Defaults match local app config (localhost:3306 root/root, db ims).
#
# Examples:
#   .\scripts\backup\backup-mysql.ps1
#   .\scripts\backup\backup-mysql.ps1 -HostName 127.0.0.1 -Port 3307 -User ims -Password ims

[CmdletBinding()]
param(
  [string] $HostName = "127.0.0.1",
  [int] $Port = 3306,
  [string] $User = "root",
  [string] $Password = "root",
  [string] $Database = "ims",
  [string] $OutDir = ""
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $false
}
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $OutDir) {
  $OutDir = Join-Path $root "backups\mysql"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$mysqldump = "C:\Program Files\MySQL\MySQL Server 8.3\bin\mysqldump.exe"
if (-not (Test-Path $mysqldump)) {
  $cmd = Get-Command mysqldump -ErrorAction SilentlyContinue
  if ($cmd) { $mysqldump = $cmd.Source } else { $mysqldump = $null }
}
if (-not $mysqldump) {
  throw "mysqldump not found. Install MySQL client or set PATH."
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outFile = Join-Path $OutDir "ims-$stamp.sql"

Write-Host "Dumping $Database from ${HostName}:$Port -> $outFile"
$env:MYSQL_PWD = $Password
try {
  & $mysqldump `
    --host=$HostName `
    --port=$Port `
    --user=$User `
    --single-transaction `
    --routines `
    --triggers `
    --hex-blob `
    --default-character-set=utf8mb4 `
    --databases $Database `
    --result-file=$outFile
  if ($LASTEXITCODE -ne 0) {
    throw "mysqldump failed with exit $LASTEXITCODE"
  }
} finally {
  Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

$sizeMb = [math]::Round((Get-Item $outFile).Length / 1MB, 2)
Write-Host "OK: $outFile ($sizeMb MB)"
Write-Output $outFile
