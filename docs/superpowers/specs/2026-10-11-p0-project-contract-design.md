# TavernDroid P0 contract and P1 specification

Status: approved by the maintainer in chat on 2026-10-11, following review of draft commit fa31005. This specifies the first standalone Android foundation described in [roadmap issue 1](https://github.com/cepeter-job/TavernDroid/issues/1). It authorizes no provider calls or release distribution. The [P1 implementation plan](../plans/2026-10-11-p1-native-foundation.md) and Native execution were subsequently approved in chat. P0 remains open until the physical reference device is finalized.

## Purpose and first acceptance example

Build a native Android roleplay app that owns characters, stories, operation recovery, and eventually local memory. Cloud generation connects directly to user-selected providers. The user can read and edit local content offline, without a TavernDroid account, server, Telegram bot, or Python runtime.

The first P1 demonstration is: install a debug APK, create a character, start a story, send a message, receive a fake streamed reply, restart the process, and resume the saved conversation. It runs with airplane mode enabled and has no Internet permission. NanoGPT and OpenRouter belong to P2; full upstream parity belongs to later phases.

## Approved decisions

| Decision | Contract and reason |
| --- | --- |
| Runtime | Kotlin engine and Compose UI; one Android runtime with explicit lifecycle ownership |
| Storage | Room canonical history, app-private files, DataStore non-secret preferences |
| Scope | Four modules and P1 fake-provider flow; later phases retain their roadmap boundaries |
| Minimum OS | Android 8.0, API 26 |
| Compile and target SDK | 37, using stable SDK package android-37.0 |
| Application ID | io.github.cepeterjob.taverndroid, debug suffix .debug; freeze before alpha distribution |
| License | GPL-3.0-only; approved and installed in the root LICENSE |
| Toolchain | AGP 9.1.1, Gradle 9.3.1, Kotlin/Compose compiler 2.4.21, Compose BOM 2026.09.00, JBR 25.0.3, bytecode target 17 |
| Physical benchmark | Record an ARM64 phone's model, OS build and RAM; 4 GB is the roadmap baseline. An emulator cannot satisfy this decision |

The issue already selects native Kotlin over the alternatives below. This contract preserves that direction instead of reopening the product scope.

| Approach | Assessment |
| --- | --- |
| Native Kotlin UI and engine | Selected; behavioral ports need tests, but Android lifecycle and storage have one owner |
| Native UI with embedded Python | Extra packaging and interoperability work; only reconsider through a separate measured proposal |
| APK with required VPS backend | Retains a server dependency and fails the standalone requirement |

Exclude Telegram transport, Mini App hosting, systemd, mandatory remote memory, cloud sync, speech, image generation, advanced group simulation, and on-device inference from P1. Do not copy upstream deployment tools into this repository.

## Module responsibilities

| Module | Type and ownership | Dependencies |
| --- | --- | --- |
| :engine | Kotlin JVM models, repository/provider/credential contracts, generation use case and identity rules | Kotlin and coroutines only |
| :storage | Android library; Room, assets, DataStore, Keystore-backed credential implementation | :engine; Android persistence APIs |
| :providers | Kotlin JVM; deterministic fake provider in P1, HTTP adapters in P2 | :engine |
| :app | Android app; Compose, ViewModels, navigation, foreground coordination and composition root | :engine, :storage, :providers |

The engine never imports Android, Room, SQL, HTTP adapters, or UI types. Contracts belong to the engine and implementations depend on those contracts. Inject implementations through constructors in the app composition root. Start screens as packages in :app. Do not introduce a dependency-injection framework or convention-plugin project for four modules.

## P1 user experience

Stories and Characters are the working navigation destinations. Library and Settings have deliberate empty states; Settings supports theme selection and explains that the current build uses a fake provider. No credential-entry or connection-test UI appears before P2.

A character has a required name and optional description and greeting. Limits are 80, 16000 and 8000 UTF-16 code units respectively. Trim surrounding whitespace from the name and reject a blank name. Starting a session captures the character fields as a local snapshot; editing a global character must not change an existing session's greeting or description. P1 can omit character editing while preserving this data rule.

The story screen displays saved canonical turns separately from a streaming or saved unfinished draft. Free-text input has a 16000 UTF-16 code-unit limit and rejects blank input. Send is guarded while an operation is active; Stop cancels it. Failed, cancelled and interrupted attempts expose explicit Retry and, when a draft exists, Discard draft actions. P1 renders text safely without HTML, scripts, or executable imported content; advanced narrative formatting is deferred to its tested port.

Create the greeting once when starting an empty session. Navigation or reopening the session cannot insert it again. Display readable light/dark themes, scalable text, TalkBack labels and minimum 48 dp controls. Handle IME insets once so the input and last message remain reachable. Rotation and navigation cannot duplicate a generation.

## Canonical storage and operations

Use application-generated IDs. P1 persists Character, Session, Turn and GenerationOperation records, plus revision/incarnation fields needed to invalidate stale results. Session records retain the captured character content. The initial branch identity is explicit; branching UI and full ancestry arrive later.

Accepting Send atomically stores exactly one user turn and its operation in a short Room transaction. Provider work happens outside write transactions. A failed or interrupted generation leaves that user turn visible. Explicit Retry creates another attempt for the existing request rather than another user turn. Attempt metadata keeps retry accounting separate from canonical dialogue.

Capture session ID, branch ID, incarnation, revision, content snapshot and fake-provider configuration in the operation. Admit one interactive generation globally and at most one mutating generation per session. Use a process coordinator and transactional storage admission; database identity checks remain authoritative. This guard prevents duplicate taps, but does not promise exactly-once remote generation or billing in P2.

Operation states are queued, preparing, streaming, committing and completed, or cancelled, failed and interrupted. Stream into a bounded draft buffer, checkpoint separately from canonical turns, and revalidate identity and the operation's non-terminal status before publishing. Commit the assistant turn and completed operation together. A late result after cancellation, reset, deletion, edit or branch change must not publish or recreate deleted data. UI redraw and delivery failures must never initiate another generation.

P1's fake provider returns a deterministic reply with controlled chunk timing and cancellation. Cap its draft at 32000 UTF-16 code units. Persist draft progress at most once per second or on terminal transition, and batch UI updates at most ten times per second. Inject clocks and dispatchers into tests rather than asserting wall-clock sleeps.

An app-scoped coordinator owns the foreground generation. ViewModels observe persisted state; they do not create a new provider request during collection or recreation. Rotation preserves the active operation. When the application genuinely backgrounds, cancel generation and save its draft; rotation must not be mistaken for backgrounding. Startup marks unfinished operations interrupted before admitting a new request. Never replay an uncertain request automatically after process death.

Stop and genuine backgrounding produce cancelled; provider/storage failure produces failed; startup recovery of unfinished work produces interrupted. These terminal attempts preserve any saved draft and offer explicit Retry and Discard draft. Retry is admitted only while the original request still belongs to the current session incarnation and transcript revision; otherwise explain that the request is stale. It adds an attempt for the existing user turn. Discard removes draft content, not canonical history. A completed operation is recovered from storage and never offered as an uncertain retry. If completion wins a race with Stop, retain the completed result. Use Room schema version 1 with exported schema files. Future migrations must preserve canonical history; destructive fallback is forbidden. Do not invent an unused migration just to create a test.

## Credentials and privacy

Define the credential boundary before P2. In P1, implement and instrument-test encryption, read, replacement and deletion using an Android Keystore protected key and app-private ciphertext, with synthetic secrets only. Authentication failure or invalidated keys must return an actionable re-entry state and never fall back to plaintext.

Exclude credentials and private stories from automatic cloud backup and device transfer using the appropriate platform controls across the supported SDK range. Store no maintainer secrets, transcript logs or release keys. The Room sandbox is the P1 storage security baseline; do not claim database encryption or end-to-end encryption. A later explicit backup/export has its own reviewed format and tests.

## Content scope and upstream evidence

P1 accepts manual local characters only; no PNG, JSON, ST preset, or STTB database import is claimed. P2 adds a versioned TavernDroid export/import round trip. P3 specifies supported card formats before implementation. Full SillyTavern chat-completion preset exports are not automatically compatible: pinned upstream skips modular exports containing prompts or prompt_order.

The [parity matrix](../../provenance/parity-matrix.md) identifies source paths and contracts at commit 38b17d10d7630baec9efe53320ca94f8ab122bda. Synthetic fixtures describe target invariants; they are not executed upstream golden outputs and establish no parity claim until adapters and tests exist.

## Acceptance tests for P1

| Public behavior | Required evidence |
| --- | --- |
| Offline local flow | Debug APK on API 26 and API 37: create character/session, fake reply, restart, recover exact conversation without network permission |
| Session isolation | Two sessions with different snapshots never mix turns, drafts or preferences |
| Admission and retry | Duplicate Send produces one user turn and one admitted operation; Retry adds an attempt without duplicating the user turn |
| Atomic publication | Inject storage failure during assistant commit; no half-written assistant/completed-operation pair appears |
| Stale result rejection | Complete a captured operation after reset/deletion/revision change; no canonical turn or resurrected session appears |
| Recovery and cancellation | Rotate, background, Stop and kill the process during streaming; recover saved data/draft, require explicit retry and do not replay automatically |
| Storage schema | Export version 1 and test repositories/transactions; future schema changes add genuine migration preservation tests |
| Secret storage | Real Android Keystore test with synthetic values; ciphertext lacks plaintext; replace/delete/invalidation work; backups exclude sensitive paths |
| Accessible UI | Compose instrumentation plus device checks for scaled text, TalkBack labels, 48 dp targets and keyboard/system-bar reachability |

Pure Kotlin tests cover engine invariants and fake streams; Android storage tests cover Room transactions and Keystore; Compose/device tests cover lifecycle and UI. CI assembles a debug APK and runs focused tests and Android lint. Exact library pins, discovered Gradle tasks, wrapper checksum, and CI action SHAs belong to the written implementation plan and its first scaffold review.

## Benchmark policy

Keep the roadmap's proposed targets: p95 cold start at most 2 seconds over 30 release launches; chat PSS at most 250 MB on a fixed text/portrait fixture; p95 send-to-working-state at most 100 ms. Measure provider latency separately. Record the physical device model, Android build, RAM, APK/source revision and fixture before measurements.

These are proposed release gates, not P0 measurements or emulator results. If the chosen phone differs from the 4 GB baseline, record and approve that difference. No benchmark claim can close while the physical reference is unspecified.

## P0 exit and review handoff

| Requirement | Current evidence or remaining action |
| --- | --- |
| Architectural direction and exclusions | Approved in chat on 2026-10-11 |
| README identity | Standalone TavernDroid wording with accurate pre-implementation status |
| License and attribution | Approved GPL-3.0-only grant, complete root LICENSE, and pinned provenance review |
| Contribution and security policy | Local policies; verify private security-reporting route before P2 |
| Parity and dependencies | Pinned source/test matrix, direct dependency review, complete runtime lock inventory, synthetic fixtures |
| Stable toolchain and Windows setup | Approved exact pins and installed SDK evidence; real build belongs to P1 |
| Device and import boundaries | API 26 floor and manual-only P1 scope approved; physical reference awaiting maintainer selection |
| Written implementation plan | Approved in chat on 2026-10-11; Native execution selected |

The maintainer approved this scoped specification and the focused P1 plan in chat. Finalize the reference-device entry before closing P0. This approval does not authorize the entire P0 through P6 roadmap as one implementation. GitHub issue checkboxes remain unchanged while the remaining exit evidence is pending.
