param([string]$Root = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
try {
    $wrapper = Join-Path ([IO.Path]::GetFullPath($Root)) 'gradle/wrapper'
    $jarHash = (Get-FileHash (Join-Path $wrapper 'gradle-wrapper.jar') -Algorithm SHA256).Hash
    if ($jarHash -ne 'b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13') {
        throw 'JAR checksum mismatch'
    }
    $properties = @{}
    foreach ($line in Get-Content (Join-Path $wrapper 'gradle-wrapper.properties')) {
        if ($line -match '^\s*(#|$)') { continue }
        if ($line -notmatch '^([^=]+)=(.*)$') { throw 'Malformed wrapper property' }
        $key = $Matches[1].Trim()
        if ($properties.ContainsKey($key)) { throw 'duplicate property' }
        $properties[$key] = $Matches[2].Trim().Replace('\:', ':')
    }
    if ($properties['distributionUrl'] -ne 'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip') {
        throw 'distribution URL mismatch'
    }
    if ($properties['distributionSha256Sum'] -ne 'b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06') {
        throw 'distribution checksum mismatch'
    }
    Write-Output 'Gradle 9.3.1 wrapper JAR and distribution integrity pins verified.'
    exit 0
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
