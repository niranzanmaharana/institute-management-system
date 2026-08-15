# Generate Postman collection from running IMS OpenAPI

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "postman"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$openApiUrl = if ($env:IMS_OPENAPI_URL) { $env:IMS_OPENAPI_URL } else { "http://localhost:8080/v3/api-docs" }
$openApiFile = Join-Path $outDir "openapi.json"
$collectionFile = Join-Path $outDir "ims-api.postman_collection.json"

Write-Host "Fetching OpenAPI from $openApiUrl"
Invoke-WebRequest -Uri $openApiUrl -OutFile $openApiFile

Write-Host "Converting to Postman collection..."
npx --yes openapi-to-postmanv2 -s $openApiFile -o $collectionFile -p

Write-Host "Wrote $collectionFile"
Write-Host "Import this file in Postman. Set header X-Institute-Id=1 for DEMO_A (Phase 0)."
