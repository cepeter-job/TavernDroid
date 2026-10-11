# TavernDroid

TavernDroid is a planned standalone Android roleplay application with a native Kotlin engine and Jetpack Compose interface. Characters, stories, and memory stay on the device. Cloud generation connects directly to the user's chosen provider.

No VPS, Telegram bot, SillyTavern server, or embedded Python runtime is required. Reading and editing local content work offline; cloud generation needs connectivity and a provider credential.

## Project status

P0 is in design review. There is no application source or installable APK yet. The first implementation will be P1's offline flow: create a character, start a story, receive a fake streamed reply, and recover the saved conversation after restart.

| Read next | Purpose |
| --- | --- |
| [P0 project contract and P1 specification](docs/superpowers/specs/2026-10-11-p0-project-contract-design.md) | Proposed decisions and acceptance requirements |
| [Windows development](docs/development/windows-setup.md) | Installed tools, exact pins, and device matrix |
| [Upstream parity](docs/provenance/parity-matrix.md) | Pinned behaviors, source paths, and test contracts |
| [Licensing and provenance](docs/provenance/licensing.md) | Proposed license and reuse rules |
| [Roadmap issue](https://github.com/cepeter-job/TavernDroid/issues/1) | P0 through P7 delivery boundaries |

## Contribution and security

Read [CONTRIBUTING.md](CONTRIBUTING.md) before proposing implementation changes and [SECURITY.md](SECURITY.md) before reporting a vulnerability. The GPLv3 licensing proposal requires maintainer approval before upstream material is ported; see the provenance review. This repository does not yet grant a separate project license.
