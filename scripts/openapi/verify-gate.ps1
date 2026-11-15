# Step 7.5 local proof: intentional breaking change fails; major/minor bump allows it.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$base = Join-Path $root "docs\api\openapi.json"
$tmpDir = Join-Path $env:TEMP "ims-openapi-gate"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
$broken = Join-Path $tmpDir "broken.json"
$bumped = Join-Path $tmpDir "bumped.json"

python -c @"
import json, pathlib, copy
base = pathlib.Path(r'$($base -replace '\\','/')')
data = json.loads(base.read_text(encoding='utf-8'))
# Remove a real path to force a breaking change.
paths = data.get('paths') or {}
victim = next((p for p in paths if p.startswith('/api/v1/')), None)
assert victim, 'no /api/v1 path to remove'
del paths[victim]
pathlib.Path(r'$($broken -replace '\\','/')').write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
data2 = copy.deepcopy(data)
# Below 1.0.0, oasdiff treats minor as the breaking channel (0.1.0 -> 0.2.0).
ver = data2['info']['version']
parts = ver.split('.')
parts[1] = str(int(parts[1]) + 1)
parts[2] = '0'
data2['info']['version'] = '.'.join(parts[:3])
pathlib.Path(r'$($bumped -replace '\\','/')').write_text(json.dumps(data2, indent=2) + '\n', encoding='utf-8')
print('removed path', victim)
print('bumped version', ver, '->', data2['info']['version'])
"@

$check = Join-Path $PSScriptRoot "check-breaking.ps1"

Write-Host "`n=== Expect FAIL: break without version bump ==="
& $check -Base $base -Revision $broken -FailOn MISSING-VERSION-BUMP
$failCode = $LASTEXITCODE
if ($failCode -eq 0) {
  throw "Gate did not fail on breaking change without version bump"
}
Write-Host "OK: gate failed as expected (exit $failCode)"

Write-Host "`n=== Expect PASS: break with version bump ==="
& $check -Base $base -Revision $bumped -FailOn MISSING-VERSION-BUMP
if ($LASTEXITCODE -ne 0) {
  throw "Gate should pass when version is bumped for breaking change"
}
Write-Host "OK: gate passed with version bump"

Write-Host "`nStep 7.5 local verification PASSED"
