param([string]$Root = (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent))
$ErrorActionPreference = 'Stop'
$hostExe = (Get-Process -Id $PID).Path
$suite = (Join-Path $Root 'tools/tests/verify-wrapper.tests.ps1').Replace("'", "''")
$quotedRoot = $Root.Replace("'", "''")
# GitHub's PowerShell runner forwards LASTEXITCODE after invoking the step script.
$command = "& '$suite' -Root '$quotedRoot'; " +
    'if (Test-Path variable:LASTEXITCODE) { exit $LASTEXITCODE }'
$output = & $hostExe -NoProfile -Command $command 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Passing wrapper contracts failed the GitHub entrypoint: $output"
}
if ("$output" -notmatch 'four tampered fixtures rejected') {
    throw 'The wrapper contracts did not finish'
}
$missingRoot = (Join-Path $Root "build/missing-wrapper-$([guid]::NewGuid())").Replace("'", "''")
$invalidCommand = "& '$suite' -Root '$missingRoot'; " +
    'if (Test-Path variable:LASTEXITCODE) { exit $LASTEXITCODE }'
$output = & $hostExe -NoProfile -Command $invalidCommand 2>&1
if ($LASTEXITCODE -eq 0) { throw 'An actual wrapper contract failure was masked' }
Write-Output 'CI entrypoint contracts: passing suite exits zero; actual failure stays nonzero.'
exit 0
