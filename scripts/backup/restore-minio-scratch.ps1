# Restore a local MinIO backup folder into a disposable scratch MinIO on ports 9100/9101.
# Does NOT overwrite the live ims-minio volume.
#
# Examples:
#   .\scripts\backup\restore-minio-scratch.ps1 -BackupDir backups\minio\yyyyMMdd-HHmmss
#   .\scripts\backup\restore-minio-scratch.ps1 -BackupDir ... -KeepContainer

[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string] $BackupDir,
  [string] $ContainerName = "ims-minio-scratch",
  [int] $ApiPort = 9100,
  [int] $ConsolePort = 9101,
  [string] $Bucket = "ims-private",
  [switch] $KeepContainer
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $false
}
if (-not (Test-Path $BackupDir)) {
  throw "Backup dir not found: $BackupDir"
}

$mount = (Resolve-Path $BackupDir).Path -replace '\\', '/'
if ($mount -match '^([A-Za-z]):/(.*)$') {
  $drive = $Matches[1].ToLower()
  $mount = "/$drive/$($Matches[2])"
}

Write-Host "Removing previous scratch MinIO (if any)..."
cmd.exe /c "docker rm -f $ContainerName >nul 2>&1" | Out-Null

Write-Host "Starting scratch MinIO on API :$ApiPort / console :$ConsolePort..."
docker run -d `
  --name $ContainerName `
  -e MINIO_ROOT_USER=minio `
  -e MINIO_ROOT_PASSWORD=minio12345 `
  -p "${ApiPort}:9000" `
  -p "${ConsolePort}:9001" `
  minio/minio:latest `
  server /data --console-address ":9001" | Out-Null

$deadline = (Get-Date).AddSeconds(60)
do {
  Start-Sleep -Seconds 2
  try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:$ApiPort/minio/health/live" -UseBasicParsing -TimeoutSec 2
    if ($r.StatusCode -eq 200) { break }
  } catch { }
  if ((Get-Date) -gt $deadline) { throw "Scratch MinIO did not become ready." }
} while ($true)

$endpoint = "http://host.docker.internal:$ApiPort"

Write-Host "Creating bucket $Bucket on scratch..."
docker run --rm `
  -e AWS_ACCESS_KEY_ID=minio `
  -e AWS_SECRET_ACCESS_KEY=minio12345 `
  -e AWS_DEFAULT_REGION=us-east-1 `
  amazon/aws-cli:latest `
  s3 mb "s3://$Bucket" --endpoint-url $endpoint 2>$null | Out-Null

Write-Host "Uploading objects from $BackupDir..."
docker run --rm `
  -e AWS_ACCESS_KEY_ID=minio `
  -e AWS_SECRET_ACCESS_KEY=minio12345 `
  -e AWS_DEFAULT_REGION=us-east-1 `
  -v "${mount}:/backup" `
  amazon/aws-cli:latest `
  s3 sync /backup "s3://$Bucket" `
  --endpoint-url $endpoint `
  --only-show-errors | Out-Null

if ($LASTEXITCODE -ne 0) {
  throw "MinIO restore sync failed with exit $LASTEXITCODE"
}

$listing = docker run --rm `
  -e AWS_ACCESS_KEY_ID=minio `
  -e AWS_SECRET_ACCESS_KEY=minio12345 `
  -e AWS_DEFAULT_REGION=us-east-1 `
  amazon/aws-cli:latest `
  s3 ls "s3://$Bucket" --recursive --endpoint-url $endpoint 2>$null

$objectCount = if ($listing) { @($listing).Count } else { 0 }
$localCount = (Get-ChildItem -Path $BackupDir -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count

$result = [pscustomobject]@{
  BackupDir     = (Resolve-Path $BackupDir).Path
  ApiPort       = $ApiPort
  ConsolePort   = $ConsolePort
  ContainerName = $ContainerName
  LocalFiles    = $localCount
  RestoredObjects = $objectCount
  RestoredAtUtc = (Get-Date).ToUniversalTime().ToString("o")
}

Write-Host ($result | Format-List | Out-String)

if (-not $KeepContainer) {
  Write-Host "Removing scratch MinIO $ContainerName..."
  docker rm -f $ContainerName | Out-Null
}

$result
