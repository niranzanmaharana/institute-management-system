# Backup MinIO bucket ims-private to backups/minio/<stamp>/ via AWS CLI (S3 API).
# Defaults match infra/docker-compose.yml local MinIO.
#
# Examples:
#   .\scripts\backup\backup-minio.ps1
#   .\scripts\backup\backup-minio.ps1 -Endpoint http://127.0.0.1:9000

[CmdletBinding()]
param(
  [string] $Endpoint = "http://host.docker.internal:9000",
  [string] $AccessKey = "minio",
  [string] $SecretKey = "minio12345",
  [string] $Bucket = "ims-private",
  [string] $OutDir = ""
)

$ErrorActionPreference = "Stop"
if ($PSVersionTable.PSVersion.Major -ge 7) {
  $PSNativeCommandUseErrorActionPreference = $false
}
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
if (-not $OutDir) {
  $OutDir = Join-Path $root "backups\minio\$stamp"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$mount = (Resolve-Path $OutDir).Path -replace '\\', '/'
# Docker Desktop on Windows: convert D:\path -> /d/path for bind mount
if ($mount -match '^([A-Za-z]):/(.*)$') {
  $drive = $Matches[1].ToLower()
  $mount = "/$drive/$($Matches[2])"
}

Write-Host "Syncing s3://$Bucket from $Endpoint -> $OutDir"
docker run --rm `
  -e "AWS_ACCESS_KEY_ID=$AccessKey" `
  -e "AWS_SECRET_ACCESS_KEY=$SecretKey" `
  -e "AWS_DEFAULT_REGION=us-east-1" `
  -v "${mount}:/backup" `
  amazon/aws-cli:latest `
  s3 sync "s3://$Bucket" /backup `
  --endpoint-url $Endpoint `
  --no-verify-ssl `
  --only-show-errors | Out-Null

if ($LASTEXITCODE -ne 0) {
  throw "MinIO backup (aws s3 sync) failed with exit $LASTEXITCODE"
}

$fileCount = (Get-ChildItem -Path $OutDir -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
Write-Host "OK: $OutDir ($fileCount objects)"
# Single pipeline object for callers that capture output
Write-Output $OutDir
