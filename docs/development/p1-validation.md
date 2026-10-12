# Native P1 validation

The approved native P1 implementation provides character creation, local stories,
deterministic streaming replies, Stop/background cancellation, explicit Retry and
discard of unfinished drafts, and persistent system/light/dark themes. It has no
Internet permission, paid provider or credential-entry UI. This is a debug build,
not a production release or a declaration that every roadmap gate is complete.

## Reproduce

Use the versions in `toolchain.json`. Local validation uses Windows, Android Studio
JBR 25.0.3, SDK `platforms;android-37.0`, Build Tools 36.0.0 and WHPX emulators.
Set `JAVA_HOME`, `ANDROID_HOME`, and the intended `ANDROID_SERIAL` explicitly.

```powershell
pwsh -NoProfile -File tools/tests/verify-wrapper.tests.ps1
.\gradlew.bat :engine:test :providers:test :storage:testDebugUnitTest :app:testDebugUnitTest ktlintCheck
.\gradlew.bat :storage:lintDebug :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :storage:connectedDebugAndroidTest :app:connectedDebugAndroidTest
pwsh -NoProfile -File tools/verify-process-recovery.ps1 -DeviceSerial emulator-5556 -DebugApk app/build/outputs/apk/debug/app-debug.apk -TestApk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

Check each command's exit code. Repeat connected tests and the host script for
API 26 and API 37; do not select an unrelated phone automatically. APK:
`app/build/outputs/apk/debug/app-debug.apk`, application ID
`io.github.cepeterjob.taverndroid.debug`, min/target SDK 26/37.

## Observed evidence

Observed 2026-10-12 local results:

| Check | Actual result |
| --- | --- |
| JVM contracts | 34 passed: engine 13, providers 3, app 18; storage JVM task has no tests |
| API 26 connected | 27 storage + 9 app passed; 2 host-only entry points skipped |
| API 37 connected | 27 storage + 9 app passed; 2 host-only entry points skipped |
| Host SIGKILL/relaunch | Queued, Streaming and Completed passed on both APIs; 6 independent verifications |
| Offline flow | API 37 airplane mode enabled: create/send/reopen passed; radio setting restored |
| Integrity, lint, packaging | Approved wrapper accepted, four tampered fixtures rejected; ktlint/build pass; lint 0 errors and 3 version advisories |

The API 26 host script initially failed because `run-as` cannot execute the shell
builtin `test`; reading the readiness file with `cat` fixed the host probe, and
all three cases passed. This was not an Android recovery failure. Android Gradle
Plugin's console prints 12 completions for the app when two assumptions skip;
the original XML had 10 tests, 2 skipped, 0 failures: 8 actual passes. After the
review regression was added, XML has 11 tests, 2 skipped, 0 failures: 9 passes;
the console similarly prints 13 completions. Counts above use XML.

Devices: `TavernDroid_API26`, Android 8.0.0, x86_64,
`Android/sdk_gphone_x86_64/generic_x86_64:8.0.0/OSR1.180418.026/6741039:userdebug/dev-keys`;
`TavernDroid_API37`, Android 17, x86_64,
`google/sdk_gphone64_x86_64/emu64xa:17/CE2A.260420.019/15611780:userdebug/dev-keys`.
Both use 2 GB emulated RAM with Windows WHPX, not the physical ARM64 benchmark baseline.

Debug APK SHA256:
`2fb46fa29e16f639f34d04b0cbbbaca04ddc6990caf39908df0541b56f0618db`.
Tested implementation revision: `779dfa83a0aa046c59e8cc4c72ef000f558f549f`
on `feat/p1-native-foundation`. Documentation updates do not change the APK.
The [fresh branch review and corrections](p1-review.md) record three Important
findings, all fixed with observed failing regressions and a passing full matrix.
No Critical or Minor findings were raised. Hosted CI and physical release gates
remain open as described below.

The real Room suite covers atomic admission/completion rollback, global admission,
first-terminal wins, stale operation identity, duplicate client commands, retry
without duplicate user turns, late checkpoints and reopening unfinished/completed
operations. Keystore tests exercise real AES-GCM on each supported emulator,
including tampering, absent keys, explicit replacement, profile binding and random
ciphertext. Missing-key tests do not establish every vendor invalidation scenario.

## Process death contract

The host script validates both APK package IDs, installs only the synthetic debug
packages, and seeds a unique session without clearing existing app data. A
test-only controlled provider emits twice across a 1.2-second checkpoint interval;
the seed writes its readiness marker only after observing the durable Room draft.
The host sends SIGKILL to that debug package's PID while instrumentation remains
alive. It requires the intentional `Process crashed` result, rejects ordinary
assertion failures, relaunches the Activity and runs a fresh verifier with a
30-second deadline. It never treats an ADB exit code alone as a passing test.

Three cases check queued admission before worker entry, streaming after a durable
checkpoint, and completed publication before acknowledgment loss. The first two
must become Interrupted with one preserved user turn and no assistant; the third
must retain Completed and exactly one assistant. Every fresh verifier requires
zero provider invocations. This uses the production Room/coordinator/startup
implementation with a test-only provider; the shipped APK uses the fake provider.
The completion seed directly invokes the real atomic publication boundary to
simulate acknowledgment loss, rather than relying on a UI timing race.

Normal connected test runs deliberately skip the two host-phase entry points;
they run only with explicit seed/verify arguments. This is abrupt process death,
distinct from Activity recreation and graceful background cancellation.

RED evidence: accepting a tampered wrapper failed the contract; temporarily
replacing `recoverUnfinished()` with `0` failed the host check with
`expected Interrupted but was Queued`. The real implementation was restored
before passing runs. No test-disabling flag ships in the application.

## Privacy, accessibility and limitations

The merged APK has no Internet permission. Backup is disabled, with exclusions in
both legacy rules and API 31+ cloud/device-transfer rules. On API 37, LocalTransport
rejected an installed debug package containing a synthetic file canary with
`Backup is not allowed`. Original backup settings and transport were restored.
Restore payloads and OEM device transfer remain untested; static exclusions and
one transport's eligibility result are not universal transfer guarantees.

Manual API 37 checks covered light/dark settings, font scales 1.0/2.0, keyboard
reachability, gesture and three-button navigation. API 26 ran the full suite with
three-button navigation and accessibility geometry checks at font scale 2.0.
TalkBack was bound and labelled controls received accessibility focus; spoken
output was not assessed. Emulator settings were restored. Functional Save/Send
tests activate their public accessibility actions because external keyboard
animations are outside Compose's idle clock. Separate 48 dp/IME geometry tests
and actual keyboard/touch checks provide layout evidence. A dedicated platform
touch/animation suite remains a release improvement.

Room schema 1 is exported under `storage/schemas`; no destructive migration
fallback is configured. The development schema has not previously shipped as a
release. Future distributed schema upgrades require actual migrations.
The APK packages the GPL license and [resolved dependency notices](../provenance/android-dependencies.md).

The pinned GitHub workflow uses read-only permissions, no provider/signing secrets,
full-history redacted Gitleaks and API 26/37 emulator jobs. Only debug APKs and test,
lint and formatting reports are uploaded. The workflow has not run on GitHub while
this branch remains local. Emulator/system-image package updates are not locked
by SDK Manager; Gradle dependencies/actions/scanner archives are pinned.

No physical ARM64 reference phone has been selected. Startup time, PSS, low-memory
behavior, audible screen-reader experience and OEM transfer are release gates,
not measured successes. The local source branch is retained for review; no remote
roadmap checkboxes or release status were changed.
