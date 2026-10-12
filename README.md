# TavernDroid

TavernDroid is a standalone Android roleplay application with a native Kotlin engine and Jetpack Compose interface. Characters and stories stay on the device.

The P1 debug build works offline with deterministic synthetic replies. Create a character, start a story, and send a message. Stop and backgrounding preserve unfinished drafts; restarting requires explicit Retry. Settings supports system, light and dark themes.

## Project status

The P0 specification and P1 plan are approved. All five native P1 implementation tasks and local Android 8/17 checks are complete; the three findings from final branch review are fixed and covered by regressions. The physical benchmark phone remains to be selected. This build has no network permission or credential-entry UI; real providers are scheduled for P2.

With the approved JDK and SDK installed, run `.\gradlew.bat :app:assembleDebug`. Install `app/build/outputs/apk/debug/app-debug.apk` on Android 8.0 or newer.

| Read next | Purpose |
| --- | --- |
| [Approved specification](docs/superpowers/specs/2026-10-11-p0-project-contract-design.md) and [P1 plan](docs/superpowers/plans/2026-10-11-p1-native-foundation.md) | Accepted decisions and approved implementation plan |
| [Windows development](docs/development/windows-setup.md) | Installed tools, exact pins, and device matrix |
| [P1 validation](docs/development/p1-validation.md) | Actual test counts, process-death evidence and remaining release gates |
| [Upstream parity](docs/provenance/parity-matrix.md) | Pinned behaviors, source paths, and test contracts |
| [Licensing and provenance](docs/provenance/licensing.md) | Approved license and reuse rules |
| [Roadmap issue](https://github.com/cepeter-job/TavernDroid/issues/1) | P0 through P7 delivery boundaries |

## Contribution and security

Read [CONTRIBUTING.md](CONTRIBUTING.md) before proposing implementation changes and [SECURITY.md](SECURITY.md) before reporting a vulnerability. TavernDroid is licensed under [GPL-3.0-only](LICENSE); see the provenance review for upstream attribution and reuse requirements.
