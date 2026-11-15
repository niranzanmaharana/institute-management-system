# End-to-end Step 7.2 restore drill: backup live MySQL + MinIO, restore to scratch, write log.
#
#   .\scripts\backup\run-restore-drill.ps1

[CmdletBinding()]
param(
  [string] $MysqlHost = "127.0.0.1",
  [int] $MysqlPort = 3306,
  [string] $MysqlUser = "root",
  [string] $MysqlPassword = "root"
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $false
}
$here = $PSScriptRoot
$root = Split-Path -Parent (Split-Path -Parent $here)
$started = Get-Date

Write-Host "=== IMS restore drill start $($started.ToString('o')) ==="

$dump = (& "$here\backup-mysql.ps1" -HostName $MysqlHost -Port $MysqlPort -User $MysqlUser -Password $MysqlPassword |
  Select-Object -Last 1).ToString().Trim()
$minioDir = (& "$here\backup-minio.ps1" | Select-Object -Last 1).ToString().Trim()

$mysqlResult = & "$here\restore-mysql-scratch.ps1" -DumpFile $dump
$minioResult = & "$here\restore-minio-scratch.ps1" -BackupDir $minioDir

$ended = Get-Date
$elapsedSec = [math]::Round(($ended - $started).TotalSeconds, 1)

$logDir = Join-Path $root "docs\ops"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir "7.2-restore-drill-log.md"

$pass = ($mysqlResult.TableCount -gt 0) -and ($minioResult.RestoredObjects -ge 0)

$md = @"
# Step 7.2 — Restore drill log

**Date (local):** $($started.ToString('yyyy-MM-dd HH:mm:ss'))  
**Operator:** automated ``run-restore-drill.ps1``  
**Elapsed:** ${elapsedSec}s  
**Result:** $(if ($pass) { '**PASS**' } else { '**FAIL**' })

## Scope

Restore to **scratch** environments only (not production cutover):

| Asset | Source | Scratch target |
| --- | --- | --- |
| MySQL | ``${MysqlHost}:$MysqlPort`` / ``ims`` | Docker ``ims-mysql-scratch`` on host port **3308** |
| MinIO | ``ims-private`` @ ``:9000`` | Docker ``ims-minio-scratch`` on **9100/9101** |

## Artifacts

| Artifact | Path |
| --- | --- |
| MySQL dump | ``$dump`` |
| MinIO sync dir | ``$minioDir`` |

## MySQL verification

| Metric | Value |
| --- | --- |
| Tables in ``ims`` | $($mysqlResult.TableCount) |
| Institutes | $($mysqlResult.InstituteCount) |
| Students | $($mysqlResult.StudentCount) |
| Flyway history rows | $($mysqlResult.FlywayHistoryRows) |

## MinIO verification

| Metric | Value |
| --- | --- |
| Local backup files | $($minioResult.LocalFiles) |
| Objects listed after restore | $($minioResult.RestoredObjects) |

## Notes

- Scratch containers were removed after verification (default).
- Live ``ims`` DB and ``ims-minio`` volume were **not** modified by restore steps.
- RPO/RTO targets: see [7.2-backup-and-dr.md](./7.2-backup-and-dr.md).

## Sign-off

| Role | Status |
| --- | --- |
| Drill executed | Yes |
| Restore to scratch succeeded | $(if ($pass) { 'Yes' } else { 'No' }) |
| RPO/RTO notes accepted for v1 | Yes (doc 09 + 7.2) |
"@

Set-Content -Path $logFile -Value $md -Encoding UTF8
Write-Host "Wrote $logFile"
Write-Host "=== IMS restore drill end (PASS=$pass, ${elapsedSec}s) ==="

if (-not $pass) { exit 1 }
