param(
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$DeviceSerial,
    [Parameter(Mandatory)][string]$DebugApk,
    [Parameter(Mandatory)][string]$TestApk
)
$ErrorActionPreference = 'Stop'
$package = 'io.github.cepeterjob.taverndroid.debug'
$testPackage = "$package.test"
$runner = "$testPackage/io.github.cepeterjob.taverndroid.TavernTestRunner"
$logSerial = $DeviceSerial.Replace(':', '_')
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb'
$aapt = Join-Path $env:ANDROID_HOME 'build-tools/36.0.0/aapt2'
if ($IsWindows) { $adb += '.exe'; $aapt += '.exe' }
function Adb([string[]]$arguments) {
    $result = & $adb -s $DeviceSerial @arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $($arguments[0]). $result" }
    return $result
}
try {
    foreach ($item in @(@($DebugApk, $package), @($TestApk, $testPackage))) {
        $badging = & $aapt dump badging $item[0] 2>&1
        if ($LASTEXITCODE -ne 0 -or "$badging" -notmatch "package: name='$($item[1])'") {
            throw "Only the expected synthetic debug APK is allowed: $($item[1])"
        }
        Adb @('install', '-r', [IO.Path]::GetFullPath($item[0])) | Out-Null
    }
    $logs = Join-Path (Split-Path $PSScriptRoot -Parent) 'build/recovery-reports'
    New-Item -ItemType Directory -Path $logs -Force | Out-Null
    foreach ($mode in @('queued', 'streaming', 'completed')) {
        $runId = [guid]::NewGuid().ToString()
        $classRoot = 'io.github.cepeterjob.taverndroid.recovery'
        $stdout = Join-Path $logs "$logSerial-$mode-seed.txt"
        $stderr = Join-Path $logs "$logSerial-$mode-seed-errors.txt"
        $start = @{
            FilePath = $adb
            ArgumentList = @('-s', $DeviceSerial, 'shell', 'am', 'instrument', '-w', '-r',
                '-e', 'class', "$classRoot.RecoverySeedTest", '-e', 'phase', 'seed',
                '-e', 'mode', $mode, '-e', 'recoveryRunId', $runId, $runner)
            RedirectStandardOutput = $stdout
            RedirectStandardError = $stderr
            PassThru = $true
        }
        if ($IsWindows) { $start.WindowStyle = 'Hidden' }
        $seed = Start-Process @start
        $marker = "files/recovery-$runId.json"
        $deadline = [DateTime]::UtcNow.AddSeconds(30)
        $ready = $false
        while ([DateTime]::UtcNow -lt $deadline) {
            if ($seed.HasExited) { throw "Seed exited before checkpoint: $(Get-Content $stdout -Raw)" }
            $probe = & $adb -s $DeviceSerial shell run-as $package cat $marker 2>$null
            if ($LASTEXITCODE -eq 0) { $ready = $true; break }
            Start-Sleep -Milliseconds 200
        }
        if (-not $ready) { throw "Checkpoint deadline exceeded: $mode" }
        $saved = (Adb @('shell', 'run-as', $package, 'cat', $marker)) -join "`n" | ConvertFrom-Json
        if ($saved.runId -ne $runId -or $saved.mode -ne $mode) { throw 'Checkpoint identity mismatch' }
        $appPid = (Adb @('shell', 'pidof', $package)) -join ''
        if ($appPid -notmatch '^\d+$') { throw 'Expected one running synthetic debug process' }
        # SIGKILL skips background cancellation and finally blocks; never kill a different package.
        Adb @('shell', 'run-as', $package, 'kill', '-9', $appPid) | Out-Null
        if (-not $seed.WaitForExit(10000)) { throw 'Killed instrumentation did not terminate' }
        $death = Get-Content $stdout -Raw
        if ($death -notmatch 'Process crashed' -or $death -match 'FAILURES!!!|INSTRUMENTATION_STATUS_CODE: -2') {
            throw "Unexpected seed failure: $death"
        }
        Adb @('shell', 'am', 'start', '-W', '-n',
            "$package/io.github.cepeterjob.taverndroid.MainActivity") | Out-Null
        $verifyLog = Join-Path $logs "$logSerial-$mode-verify.txt"
        $verifyErrors = Join-Path $logs "$logSerial-$mode-verify-errors.txt"
        $verifyStart = @{
            FilePath = $adb
            ArgumentList = @('-s', $DeviceSerial, 'shell', 'am', 'instrument', '-w', '-r',
                '-e', 'class', "$classRoot.RecoveryVerifyTest", '-e', 'phase', 'verify',
                '-e', 'recoveryRunId', $runId, $runner)
            RedirectStandardOutput = $verifyLog
            RedirectStandardError = $verifyErrors
            PassThru = $true
        }
        if ($IsWindows) { $verifyStart.WindowStyle = 'Hidden' }
        $verifyProcess = Start-Process @verifyStart
        if (-not $verifyProcess.WaitForExit(30000)) {
            $verifyProcess.Kill()
            throw 'Recovery verification deadline exceeded'
        }
        if ($verifyProcess.ExitCode -ne 0) { throw "Verification ADB failed: $(Get-Content $verifyErrors -Raw)" }
        $verification = Get-Content $verifyLog -Raw
        if ("$verification" -notmatch 'OK \(1 test\)' -or
            "$verification" -match 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS_CODE: -2') {
            throw "Recovery contract failed: $mode. $verification"
        }
        Write-Output "$DeviceSerial ${mode}: SIGKILL/relaunch recovery passed; no replay or duplicate reply."
    }
    exit 0
} catch {
    # A failed host check must not leave its suspended instrumentation running.
    if ($seed -and -not $seed.HasExited) {
        $cleanupPid = (& $adb -s $DeviceSerial shell pidof $package 2>$null) -join ''
        if ($cleanupPid -match '^\d+$') {
            & $adb -s $DeviceSerial shell run-as $package kill -9 $cleanupPid 2>$null | Out-Null
        }
        if (-not $seed.WaitForExit(5000)) { $seed.Kill() }
    }
    Write-Error $_ -ErrorAction Continue
    exit 1
}
