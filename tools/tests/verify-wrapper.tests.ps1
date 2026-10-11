param([string]$Root = (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent))
$ErrorActionPreference = 'Stop'
$hostExe = (Get-Process -Id $PID).Path
$verifier = Join-Path $Root 'tools/verify-wrapper.ps1'
function Check([string]$target, [bool]$accepted, [string]$reason) {
    $output = & $hostExe -NoProfile -File $verifier -Root $target 2>&1
    if (($LASTEXITCODE -eq 0) -ne $accepted) { throw "Wrapper contract failed: $reason. $output" }
    if (-not $accepted -and "$output" -notmatch $reason) { throw "Unexpected rejection: $output" }
}
Check $Root $true 'approved wrapper'
$fixtureParent = [IO.Path]::GetFullPath((Join-Path $Root 'build/verification-fixtures'))
$fixture = Join-Path $fixtureParent ([guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path (Join-Path $fixture 'gradle/wrapper') -Force | Out-Null
try {
    $wrapper = Join-Path $Root 'gradle/wrapper'
    Copy-Item (Join-Path $wrapper '*') (Join-Path $fixture 'gradle/wrapper')
    $jar = Join-Path $fixture 'gradle/wrapper/gradle-wrapper.jar'
    [IO.File]::WriteAllBytes($jar, [byte[]](0, 1, 2))
    Check $fixture $false 'JAR checksum'
    Copy-Item (Join-Path $wrapper 'gradle-wrapper.jar') $jar
    $properties = Join-Path $fixture 'gradle/wrapper/gradle-wrapper.properties'
    $original = Get-Content $properties -Raw
    Set-Content $properties ($original.Replace('9.3.1-bin', '9.9-bin'))
    Check $fixture $false 'distribution URL'
    Set-Content $properties ($original.Replace('b266d5ff', '00000000'))
    Check $fixture $false 'distribution checksum'
    Set-Content $properties ($original + "`ndistributionUrl=duplicate`n")
    Check $fixture $false 'duplicate property'
    Write-Output 'Wrapper contracts: approved files accepted; four tampered fixtures rejected.'
} finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    if (-not $resolved.StartsWith($fixtureParent + [IO.Path]::DirectorySeparatorChar)) {
        throw 'Fixture cleanup escaped its parent'
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
