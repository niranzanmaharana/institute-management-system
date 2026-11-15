# Export springdoc OpenAPI into the committed contract at docs/api/openapi.json
# Requires ims-api (or gateway proxy) serving /v3/api-docs.
#
#   .\scripts\openapi\export-openapi.ps1
#   $env:IMS_OPENAPI_URL = 'http://localhost:8080/v3/api-docs'

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$outFile = Join-Path $root "docs\api\openapi.json"
$url = if ($env:IMS_OPENAPI_URL) { $env:IMS_OPENAPI_URL } else { "http://localhost:8080/v3/api-docs" }

Write-Host "Fetching OpenAPI from $url"
$raw = Invoke-RestMethod -Uri $url -Method Get
# Drop ephemeral server URL so the committed contract is host-stable.
if ($null -ne $raw.servers) {
  $raw.PSObject.Properties.Remove("servers")
}

$dir = Split-Path -Parent $outFile
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$json = $raw | ConvertTo-Json -Depth 100
# Prefer Python for stable UTF-8 pretty JSON when available.
$py = Get-Command python -ErrorAction SilentlyContinue
if ($py) {
  $tmp = Join-Path $env:TEMP "ims-openapi-export.json"
  Set-Content -Path $tmp -Value $json -Encoding utf8
  python -c @"
import json, pathlib
src = pathlib.Path(r'$($tmp -replace '\\','/')')
dst = pathlib.Path(r'$($outFile -replace '\\','/')')
data = json.loads(src.read_text(encoding='utf-8-sig'))
data.pop('servers', None)
dst.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
print('Wrote', dst, 'version=', data.get('info', {}).get('version'), 'paths=', len(data.get('paths', {})))
"@
  Remove-Item $tmp -ErrorAction SilentlyContinue
} else {
  Set-Content -Path $outFile -Value $json -Encoding utf8
  Write-Host "Wrote $outFile (PowerShell JSON; install Python for prettier output)"
}

Write-Host "Remember: breaking changes require a major bump of info.version (or minor bump while <1.0.0) in OpenApiConfig + this file."
