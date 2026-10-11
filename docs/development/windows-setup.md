# Windows development setup

Use the Windows checkout at `D:\TavernDroid` and Windows Gradle caches. Android Studio Rabbit 1 is installed at `D:\Android\AndroidStudio`; its bundled Java runtime is at `D:\Android\AndroidStudio\jbr`. The SDK is at `D:\Android\Sdk` and virtual devices are at `D:\Android\Avd`.

## Toolchain proposed for approval

[toolchain.json](toolchain.json) records the exact P0 pins. P1 must copy them into its version catalog and wrapper and verify a real build before claiming build compatibility.

| Component | Version |
| --- | --- |
| Android Studio Stable | Rabbit 1 distribution 2026.2.1.8 |
| Android Gradle plugin | 9.1.1 |
| Gradle wrapper | 9.3.1 |
| Kotlin and Compose compiler plugin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Local Gradle runtime | JBR 25.0.3 |
| JVM bytecode target | 17 |
| Compile and target SDK | 37, platform package android-37.0 |
| Build Tools | 36.0.0 |
| Proposed minimum SDK | 26, Android 8.0 |

AGP 9.1.1 explicitly supports API 37 and requires Gradle 9.3.1 and Build Tools 36.0.0. Both chosen build versions are inside Kotlin 2.4.21's published compatibility range. Gradle 9.3.1 supports Java 25; the application bytecode target remains 17. Sources: [AGP](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html), [Gradle Java](https://docs.gradle.org/current/userguide/compatibility.html).

AGP 9 uses built-in Kotlin. Keep it enabled in Android modules and do not apply `org.jetbrains.kotlin.android`. Explicitly raise AGP's Kotlin dependency to 2.4.21 using the documented buildscript dependency override. Apply `org.jetbrains.kotlin.jvm` 2.4.21 to JVM modules and the Compose compiler plugin 2.4.21 to the UI module. The Compose BOM governs library versions, not the compiler. Sources: [built-in Kotlin](https://developer.android.com/build/releases/agp-9-0-0-release-notes), [Compose BOM](https://developer.android.com/develop/ui/compose/bom).

P1 will pin Room, its code processor, coroutines, DataStore, lifecycle, navigation, and test libraries as part of the first reviewed dependency set. No dynamic versions or previews are permitted. A processor must support the selected Kotlin version. Do not install a global Gradle or standalone Kotlin compiler.

## Open and check the environment

1. Open Android Studio from Start Menu and select `D:\TavernDroid`.
2. Set the SDK directory to `D:\Android\Sdk` and Gradle JDK to the bundled runtime.
3. Restart terminal applications after changing user environment variables.
4. Run the environment checks below.
5. After P1 creates a verified wrapper, run its documented build and test commands.

```powershell
$env:JAVA_HOME = 'D:\Android\AndroidStudio\jbr'
$env:ANDROID_HOME = 'D:\Android\Sdk'
$env:ANDROID_AVD_HOME = 'D:\Android\Avd'
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$env:ANDROID_HOME --list_installed
& "$env:ANDROID_HOME\emulator\emulator.exe" -accel-check
```

Installed packages include SDK command-line tools 22.0, platform 37.0 revision 2, Build Tools 36.0.0, Platform Tools 37.0.1, Emulator 37.2.12, and Google APIs x86_64 API 37.0 image revision 6. The Pixel 7 virtual device is `TavernDroid_API37`. Its earlier setup boot check reached `sys.boot_completed=1`; this is an environment check, not an application test.

## Supported development and device matrix

| Environment or device | Planned purpose and boundary |
| --- | --- |
| Windows 11 x64 with Android Studio Stable | Primary development; checkout and caches remain on Windows storage |
| Linux x64 CI with pinned Java 25 and wrapper | Reproducible build, JVM tests, Android lint, APK assembly |
| Android API 26 emulator | Minimum SDK lifecycle and UI compatibility in P1 |
| Pixel 7 API 37 emulator | Current API instrumentation and local development in P1 |
| ARM64 physical phone with 4 GB RAM baseline | Release startup, PSS, scrolling and interruption benchmarks; model and OS require maintainer selection |

Android 8.0 through Android 17 is the proposed OS range, subject to minSdk approval and actual tests. ARM64 hardware is the release benchmark baseline; the current app design has no native inference runtime. Tablets, foldables, and vendor-specific background behavior require separate evidence before compatibility claims.

Do not use the API 37 emulator as physical performance evidence. Physical reference selection remains a P0 exit requirement. In WSL, use a separate Linux checkout and caches for optional reference tooling; do not concurrently build or mutate the Windows checkout. See [Microsoft filesystem guidance](https://learn.microsoft.com/en-us/windows/wsl/filesystems).

## Signing and updates

Proposed release application ID: `io.github.cepeterjob.taverndroid`; debug builds append `.debug`. Freeze the release ID before distributing an alpha. Debug and release credentials and data remain separate.

P1 uses Android's local debug signing identity. Create the independent release signing key only when a release is being prepared; keep it outside Git, retain controlled offline backups, and record its certificate fingerprint with release provenance. Keep one certificate and increasing version codes for in-place upgrades. Untrusted PR workflows receive no release key. App-private data retention across upgrades requires device tests.
