# TavernDroid P1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship an offline debug APK that creates characters and stories, streams a deterministic fake reply, and recovers canonical history and unfinished drafts after restart.

**Architecture:** Four modules enforce the approved direction: pure Kotlin :engine contracts and use cases; Android :storage adapters; pure Kotlin :providers with a fake stream; Compose :app with application-owned foreground coordination. Room owns durable admission and publication; providers run outside transactions. Use constructor injection and test fakes.

**Tech Stack:** Approved Kotlin 2.4.21, AGP 9.1.1, Gradle 9.3.1, Java runtime 25 with bytecode target 17, Compose BOM 2026.09.00, Room/KSP2, coroutines, DataStore, Android Keystore and JUnit.

**Spec:** [Approved P0 contract and P1 specification](../specs/2026-10-11-p0-project-contract-design.md), approved in chat on 2026-10-11. The maintainer approved this plan and Native execution in chat on 2026-10-11. Implementation proceeds in the managed taverndroid-p1 worktree on feat/p1-native-foundation.

## Global Constraints

- Android 8.0, API 26; compile and target SDK 37; platform package platforms;android-37.0; Build Tools 36.0.0.
- Application ID io.github.cepeterjob.taverndroid, debug suffix .debug; GPL-3.0-only; no Internet permission or real provider credentials in P1.
- Name, description, greeting and user-text limits are 80, 16000, 8000 and 16000 UTF-16 code units; draft cap is 32000. Trim name, reject blank name/text.
- One interactive generation globally; checkpoint at most once per second except terminal flush; batch UI at most ten times per second; no automatic replay after process death.
- Four approved modules only; no Android/Room/HTTP dependencies in :engine; no Hilt, embedded Python, server, imports, branching UI, paid provider calls or speculative migrations.

## Review Focus

- Admission commits but worker never starts: restart interrupts it and explicit retry retains the user turn. Task 2 and Task 5.
- Completion commits but acknowledgment is lost: reload shows completed output without another generation. Task 2 and Task 5.
- Stop and completion race: the first durable terminal transition wins; completed output is never downgraded. Task 2.
- A checkpoint arrives after Stop, discard or retry: it cannot restore a discarded draft or overwrite another attempt. Task 2.
- Terminal persistence fails or a key disappears: preserve the last durable state and require explicit recovery; never invent saved text or plaintext fallback. Tasks 2 and 3.

## Dependency and integrity pins

The exact approved compiler/tool pins are in [toolchain.json](../../development/toolchain.json). Use google(), mavenCentral() and gradlePluginPortal() only where appropriate; prohibit per-module repositories and dynamic versions. Pin all directly used artifacts in gradle/libs.versions.toml, lock resolved dependencies, and preserve notices in the release inventory. These selections require a real first build; research and metadata checks are not compilation evidence.

| Artifact or plugin | Pin | License and use |
| --- | --- | --- |
| androidx.room:room-runtime, room-compiler, room-testing; androidx.room plugin | 2.8.5 | Apache-2.0; Room schema 1 and exported schemas |
| com.google.devtools.ksp | 2.3.12 | Apache-2.0; KSP2 only |
| org.jetbrains.kotlinx:kotlinx-coroutines-core, android, test | 1.11.0 | Apache-2.0; structured concurrency and virtual time |
| androidx.datastore:datastore-preferences | 1.2.1 | Apache-2.0; theme only |
| androidx.activity:activity-compose | 1.13.0 | Apache-2.0; Compose Activity |
| androidx.lifecycle:runtime, runtime-compose, viewmodel, viewmodel-compose, process | 2.11.0 | Apache-2.0; use actual artifact names with lifecycle- prefix |
| androidx.navigation:navigation-compose | 2.10.2 | Apache-2.0; simple String routes, no serialization plugin |
| androidx.compose UI, foundation, Material3, ui-test-junit4 and ui-test-manifest | BOM 2026.09.00 | Apache-2.0; no alpha overrides; test-manifest is debug-only |
| junit:junit | 4.13.2 | EPL-1.0; test-only |
| androidx.test:runner / androidx.test.ext:junit / androidx.test.espresso:espresso-core | 1.7.0 / 1.3.0 / 3.7.0 | Apache-2.0; Android tests |
| org.jlleitschuh.gradle.ktlint / ktlint engine | 14.2.0 / 1.8.0 | MIT; pin the engine explicitly, conventional Kotlin syntax |

Room's coroutine APIs are in room-runtime; omit the empty room-ktx artifact. Room's minSdk 23 and plugin AGP 8.4 floors are met. KSP2 is compatible with AGP built-in Kotlin; keep built-in Kotlin enabled and override its KGP dependency to 2.4.21 as documented. Apply org.jetbrains.kotlin.jvm to :engine/:providers and the matching Compose compiler plugin to :app.

Published UI/foundation 1.12.1, Lifecycle Compose 2.11.0 and Navigation Compose 2.10.2 Android metadata requires compile SDK 37 and AGP 9.1.0; the approved pins satisfy it. Lifecycle release prose instead mentions AGP 9.2.0. Keep this discrepancy visible and let first-build AAR checks/compilation decide; if the full graph rejects the approved toolchain, stop and propose the smallest justified change. ktlint's parser is older than Kotlin 2.4; do not introduce new language syntax just to use it.

Primary evidence: [Room](https://developer.android.com/jetpack/androidx/releases/room), [KSP](https://github.com/google/ksp/releases/tag/2.3.12), [coroutines](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore), [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle), [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation), [Android tests](https://developer.android.com/jetpack/androidx/releases/test), [BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom), [ktlint plugin](https://github.com/JLLeitschuh/ktlint-gradle/blob/v14.2.0/CHANGELOG.md).

| Integrity input | Exact value |
| --- | --- |
| Gradle distribution | https://services.gradle.org/distributions/gradle-9.3.1-bin.zip |
| Distribution SHA256 | b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06 |
| Wrapper JAR SHA256 | b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13 |
| actions/checkout v7.0.1 | 3d3c42e5aac5ba805825da76410c181273ba90b1 |
| actions/setup-java v6.0.1 | de7274f081f381c8f8158605e0321c36c376e2e6 |
| actions/upload-artifact v7.0.2 | cf430e030ddbb5b0abf93d22962f4752f3646cd9 |
| gradle/actions v6.4.0, setup-gradle path | 3f5f9adaf7d9fecd50b5935e54106014257a94e6 |
| Gitleaks Linux x64 8.30.1 archive | 551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb |

Distribution/JAR hashes come from Gradle's published .sha256 endpoints; Gitleaks hash comes from its [release checksums](https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_checksums.txt) for [gitleaks_8.30.1_linux_x64.tar.gz](https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz). Generate the wrapper using a verified temporary Gradle distribution at execution time, set distributionSha256Sum, and verify the JAR hash before running it. Do not install global Gradle or bootstrap through an unverified wrapper.

## File structure and shared data

For the file tables below, E means engine/src/main/kotlin/io/github/cepeterjob/taverndroid/engine, S means storage/src/main/kotlin/io/github/cepeterjob/taverndroid/storage, P means providers/src/main/kotlin/io/github/cepeterjob/taverndroid/providers, and A means app/src/main/kotlin/io/github/cepeterjob/taverndroid. These are exact path prefixes. Tests use the same package under each module's src/test/kotlin or src/androidTest/kotlin. No production files exist yet; every file below is a planned creation unless explicitly marked Modify.

| File group | Responsibility |
| --- | --- |
| Root/module build files and gradle/libs.versions.toml | Approved versions, dependency boundaries, wrapper, processor/schema output and tests |
| E/Characters.kt, Story.kt, StoryRepository.kt | Input limits, immutable character/session/turn models and content persistence contract |
| E/Generation.kt, GenerationStore.kt, GenerationRunner.kt; P/FakeStreamingProvider.kt | Operation identity, durable command boundary and bounded fake generation |
| S/TavernDatabase.kt, StoryEntities.kt, StoryDao.kt, RoomStoryRepository.kt, GenerationDao.kt, RoomGenerationStore.kt | Schema, snapshots, admission, commit and recovery |
| S/KeystoreCredentialStore.kt, ThemePreferences.kt; A/AppContainer.kt and screens/coordinator | Private ciphertext/preferences, composition, lifecycle and UI |

Use String UUIDs for IDs, not an unused generic ID abstraction. CharacterInput contains name, description and greeting. Character contains id and validated content. SessionSnapshot contains SessionIdentity(sessionId, branchId, incarnation, revision) and a captured CharacterInput. Turn contains id, sessionId, ordinal, role, text and nullable logical requestId. GenerationOperation rows represent attempts: operationId, logical requestId, unique client nonce, attempt number, userTurnId, captured identity/content/configuration, state, draft and failure category. Retry inserts an attempt row referencing the existing user turn, preserving earlier attempt history. Unique request IDs on user turns and unique operation IDs on assistant turns prevent duplicate canonical inserts.

### Task 1: Build the persisted character and session foundation

**Deliverable:** A buildable four-module debug APK and tested Room-backed character/session API. Configuration exists to serve this vertical slice, not as a separate setup-only task.

| Files | Change |
| --- | --- |
| settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, gradle/wrapper/gradle-wrapper.jar, gradlew, gradlew.bat | Create exact-pinned wrapper and module graph |
| app/build.gradle.kts, engine/build.gradle.kts, storage/build.gradle.kts, providers/build.gradle.kts; app/src/main/AndroidManifest.xml; A/MainActivity.kt; app/src/main/res/values/strings.xml | Create minimal launcher, approved IDs/SDKs and test runners; no network permission |
| E/Characters.kt, Story.kt, StoryRepository.kt; S/TavernDatabase.kt, StoryEntities.kt, StoryDao.kt, RoomStoryRepository.kt | Create normalization, schema 1 and content adapters |
| engine/src/test/kotlin/io/github/cepeterjob/taverndroid/engine/CharacterValidationTest.kt; storage/src/androidTest/kotlin/io/github/cepeterjob/taverndroid/storage/StoryRepositoryTest.kt | Test public validation, snapshots, greeting identity and file-backed reopen |
| storage/schemas/; dependency lockfiles; .editorconfig | Export actual schema and lock graph; pin Kotlin formatting settings |

**Interfaces:** Produces normalizeCharacter(input: CharacterInput): CharacterInput, throwing InputValidationException(field: String) for invalid data. StoryRepository exposes suspend createCharacter(input): Character, observeCharacters(): Flow<List<Character>>, suspend startSession(characterId: String): SessionSnapshot, observeSessions(): Flow<List<SessionSnapshot>>, suspend readSession(sessionId: String): SessionSnapshot?, and observeTurns(sessionId: String): Flow<List<Turn>>. Database starts at schema version 1. startSession generates a new session ID and inserts its captured greeting once in the same transaction; reopening only reads it.

- [x] **Step 1: Establish the exact module/wrapper graph and write failing contract tests.** Assert trimmed name, blank rejection, boundaries 80/16000/8000, and emoji UTF-16 behavior: 40 two-unit emoji fit an 80-unit name, 41 do not. Room tests create two sessions, mutate a global character through a test fixture and verify snapshots stay unchanged; reopen the same file-backed DB and assert one greeting per session. Stub only the API needed to make these assertions run.

```kotlin
@Test fun nameUsesUtf16UnitsAndTrimsWhitespace() {
    val input = CharacterInput("  " + "\uD83D\uDE00".repeat(40) + "  ", "", "")
    assertEquals(80, normalizeCharacter(input).name.length)
    assertThrows(InputValidationException::class.java) {
        normalizeCharacter(input.copy(name = "\uD83D\uDE00".repeat(41)))
    }
}
```

- [x] **Step 2: Run the contracts and confirm behavior fails.** Discover projects/tasks first, then run `.\gradlew.bat :engine:test --tests '*CharacterValidationTest'` and the Room test class on an authorized emulator. Missing wrapper/dependency resolution is a setup failure to repair, not the expected red test.
- [x] **Step 3: Implement normalization and transactional persistence in the listed files.** Use Room foreign keys and stable turn ordering; do not hold a transaction while emitting a Flow. Export schema 1, preserve snapshots, and provide a minimal Activity that renders without network or credentials. Configure KSP2/Room and Kotlin targets from the approved pins; no destructive fallback.
- [x] **Step 4: Verify the slice.** Run :engine:test, :storage:connectedDebugAndroidTest, :storage:lintDebug, :app:lintDebug, :app:assembleDebug and ktlintCheck. All must exit 0; Room asserts snapshot/reopen/greeting behavior. Inspect actual dependencies and the merged manifest for forbidden module edges and network permission. Lock the resolved graph and review exported schema.
- [x] **Step 5: Commit the independently working foundation.** Stage only this task's build, source, tests, schema and lock outputs; commit `feat: add native character and session foundation`.

### Task 2: Add generation admission and durable fake-stream recovery

**Deliverable:** Engine/Room/provider contracts exchange a fake reply and survive retries, races and interruption without duplicate canonical turns.

| Files | Change |
| --- | --- |
| E/Generation.kt, GenerationStore.kt, GenerationRunner.kt; P/FakeStreamingProvider.kt | Create operation types, contracts and bounded runner |
| S/GenerationDao.kt, RoomGenerationStore.kt; S/StoryEntities.kt, TavernDatabase.kt | Create attempt rows and transactions; complete initial schema 1 before any released database exists |
| engine/src/test/kotlin/io/github/cepeterjob/taverndroid/engine/GenerationRunnerTest.kt; providers/src/test/kotlin/io/github/cepeterjob/taverndroid/providers/FakeStreamingProviderTest.kt | Test public events, virtual timing, bounds, cancellation and failures |
| storage/src/androidTest/kotlin/io/github/cepeterjob/taverndroid/storage/GenerationStoreTest.kt | Test real transaction admission/publication, retry and recovery |
| docs/provenance/fixtures/stale-generation.json; engine/src/test/resources/stale-generation.json | Read the existing synthetic contract; copy as an explicit engine test input |

**Interfaces:** validateUserText(text: String): String preserves valid text and throws InputValidationException("text") for blank input or more than 16000 UTF-16 code units. OperationSnapshot includes operationId: String, requestId: String, attemptNumber: Int, userTurnId: String, identity: SessionIdentity, character: CharacterInput, history: List<Turn>, userText: String and config: FakeProviderConfig(chunkCodeUnits: Int = 16, chunkDelayMillis: Long = 100). OpState values are Queued, Preparing, Streaming, Committing, Completed, Cancelled, Failed and Interrupted. Admission has Admitted(snapshot), AlreadyKnown(operationId, state), Busy, Stale, Missing and Terminal outcomes. CommitResult/FinishResult distinguish Accepted, Stale, Terminal and Missing. No typed outcome hides a genuine storage exception.

GenerationStore exposes suspend admitSend(sessionId: String, expectedRevision: Long, clientNonce: String, text: String): Admission; suspend admitRetry(operationId: String, clientNonce: String): Admission; suspend transition(operationId: String, from: OpState, to: OpState): Boolean; suspend checkpointDraft(operationId: String, text: String): Boolean; suspend complete(snapshot: OperationSnapshot, reply: String): CommitResult; suspend finish(operationId: String, state: OpState, draft: String, failure: FailureCategory? = null): FinishResult; suspend discardDraft(operationId: String): Boolean; suspend recoverUnfinished(): Int; observeLatestOperation(sessionId: String): Flow<OperationView?>. OperationView exposes snapshot, state, durable draft and failure: FailureCategory?; FailureCategory has Provider and Storage values, not exception text. transition accepts only Queued-to-Preparing, Preparing-to-Streaming and Streaming-to-Committing; terminal states cannot be entered or reopened through it. finish accepts only Cancelled/Failed; complete owns Completed and recovery creates Interrupted. GenerationProvider.stream(snapshot: OperationSnapshot): Flow<String> emits text chunks. GenerationRunner.run(snapshot: OperationSnapshot, onDraft: (DraftUpdate) -> Unit) is suspending; DraftUpdate contains operationId and current bounded text. Inject a monotonic clock for rate checks and a coroutine dispatcher; fake delays use the coroutine scheduler.

- [x] **Step 1: Write failing public contracts.** Room tests assert one global admission across two sessions, duplicate nonce returns AlreadyKnown, atomic user/attempt insertion, retry adds attempt 2 without a second user turn, stale incarnation/revision/deletion rejects publication, and Stop/complete cannot overwrite each other's terminal result. Assert transition rejects direct terminal writes and terminal-to-active changes for all four terminal states; attempted revival followed by complete cannot publish. Inject a throw between assistant insert and completed-state write inside the real Room transaction; assert both roll back. File-backed reopen covers queued work with no worker, completed acknowledgment loss, and durable draft recovery. JVM tests assert blank text rejection, 16000/16001-unit input boundaries and unchanged valid text; virtual time asserts 32000-unit cap, at most one checkpoint per second except terminal flush, at most ten UI updates per second, cancellation before first chunk, provider error, late chunks and a failed terminal save retaining only the last durable draft.

Use a fresh real Room database per test; repository and store below are its adapters from Tasks 1 and 2. TurnRole is User or Assistant. The greeting is an Assistant turn with no request ID.

```kotlin
@Test fun retryKeepsOneUserTurnAndCompletedCannotBeCancelled() = runTest {
    val character = repository.createCharacter(CharacterInput("Ada", "", ""))
    val session = repository.startSession(character.id)
    val first = store.admitSend(session.identity.sessionId,
        session.identity.revision, "send-1", "Hello") as Admission.Admitted
    assertEquals(FinishResult.Accepted,
        store.finish(first.snapshot.operationId, OpState.Cancelled, "Synthetic"))
    val retry = store.admitRetry(first.snapshot.operationId, "retry-1") as Admission.Admitted
    assertEquals(2, retry.snapshot.attemptNumber)
    assertEquals(CommitResult.Accepted, store.complete(retry.snapshot, "Synthetic reply: Hello"))
    assertEquals(FinishResult.Terminal,
        store.finish(retry.snapshot.operationId, OpState.Cancelled, "late"))
    val turns = repository.observeTurns(session.identity.sessionId).first()
    assertEquals(1, turns.count { it.role == TurnRole.User })
    assertEquals(1, turns.count { it.role == TurnRole.Assistant })
}
```

- [x] **Step 2: Run those tests and observe contract failures.** Use :engine:test, :providers:test and :storage:connectedDebugAndroidTest with the relevant class filter. Tests must fail on behavior, not absent Android SDK or malformed fixture JSON.
- [x] **Step 3: Implement the command boundary and runner.** Admission inserts user turn and captured attempt transactionally and increments/captures revision consistently. Unique client nonces deduplicate command replay; only Admitted starts a worker. Retry must target the latest retryable attempt whose original request still matches the current incarnation/revision. complete checks identity and non-terminal state before inserting assistant and terminalizing together. Conditional finish never downgrades Completed. Stop/failure preserves the bounded durable draft; discard clears only terminal draft content; checkpoint checks state and identity so delayed writes cannot restore it. recoverUnfinished interrupts active rows before admission and never calls a provider. The fake reply is deterministic (`Synthetic reply: ` followed by captured user text). On CancellationException, make a bounded terminal persistence attempt and rethrow; do not swallow cancellation or promise an unsaved buffer survived a storage error.
- [x] **Step 4: Verify durable outcomes.** Run the full engine/provider suites and Room transaction tests, then :app:assembleDebug and ktlintCheck. Assert exact canonical user/assistant counts and preserved prior attempt metadata. Simulate reset/edit/delete using test database mutations; do not ship reset/edit UI in P1. Review the initial schema changes before any distributed build; future upgrades need genuine migrations.
- [x] **Step 5: Commit the operation slice.** Commit `feat: add durable fake generation and explicit recovery` with its schema and fixture evidence.

### Task 3: Add credential protection and non-secret preferences

**Deliverable:** Real Keystore-backed synthetic credential tests, durable theme selection, private file paths and backup exclusions without a credential-entry UI.

| Files | Change |
| --- | --- |
| E/CredentialStore.kt, ThemePreferences.kt; S/KeystoreCredentialStore.kt, PrivatePaths.kt, ThemePreferences.kt | Create contracts and adapters |
| app/src/main/AndroidManifest.xml; app/src/main/res/xml/backup_rules.xml, data_extraction_rules.xml | Modify manifest; create explicit cloud/transfer exclusions |
| storage/src/androidTest/kotlin/io/github/cepeterjob/taverndroid/storage/KeystoreCredentialStoreTest.kt, ThemePreferencesTest.kt | Test actual Keystore, tampering, missing key, theme persistence |
| app/src/androidTest/kotlin/io/github/cepeterjob/taverndroid/PrivacyPolicyTest.kt | Test manifest flags and parsed backup rules |
| A/Diagnostics.kt; app/src/test/kotlin/io/github/cepeterjob/taverndroid/DiagnosticsTest.kt | Create bounded event-only logging and leakage assertions |

**Interfaces:** CredentialStore exposes suspend put(profileId: String, secret: String): Unit, suspend read(profileId: String): CredentialRead, suspend delete(profileId: String): Unit. CredentialRead is Absent, Available(secret) or NeedsReentry(reason); reasons include UnavailableKey and CorruptCiphertext. Never render/log an Available value. PreferencesRepository exposes theme: Flow<ThemeChoice> and suspend setTheme(choice: ThemeChoice): Unit; ThemeChoice is System, Light or Dark. PrivatePaths is Android-owned and supplies the no-backup credential directory plus app-private asset directory. Diagnostics accepts a closed event code, operation ID and exception category; it has no transcript/credential argument.

- [x] **Step 1: Write failing Android security/preferences contracts.** Synthetic values round-trip through real Keystore, explicit replacement works, deletion returns Absent, ciphertext does not contain the plaintext, tampering returns NeedsReentry, and deleting the key while keeping ciphertext returns NeedsReentry without silently overwriting it. Reopen DataStore to prove theme persistence/default System. Privacy tests require allowBackup=false and exclusions for cloud backup and device transfer as well as pre-31 backup paths. Logging tests assert synthetic secret and transcript canaries never appear.

Use a unique profile and test-only key/file namespace; clean it in teardown without touching user data.

```kotlin
@Test fun syntheticCredentialCanBeReplacedAndDeleted() = runTest {
    credentials.put("test-profile", "synthetic-canary-1")
    assertEquals(CredentialRead.Available("synthetic-canary-1"), credentials.read("test-profile"))
    credentials.put("test-profile", "synthetic-canary-2")
    assertEquals(CredentialRead.Available("synthetic-canary-2"), credentials.read("test-profile"))
    credentials.delete("test-profile")
    assertEquals(CredentialRead.Absent, credentials.read("test-profile"))
}
```

- [x] **Step 2: Run and confirm behavior failures.** Run the named storage Android tests and app privacy/logging tests. Key deletion establishes the missing-key case; it is not evidence for every biometric or vendor invalidation path.
- [x] **Step 3: Implement minimal private adapters.** Encrypt using Android Keystore AES-GCM with fresh IVs and atomic ciphertext replacement under noBackupFilesDir. Derive filenames from profile IDs instead of accepting file paths. Keep assets app-private; add no unused import/cache abstraction. Corruption/missing keys require explicit re-entry; explicit put may replace them, read may not. Use one application DataStore instance. Exclude sensitive database/file/preferences paths with fullBackupContent before API 31 and dataExtractionRules cloud-backup plus device-transfer sections on API 31+. No plaintext fallback or logging of low-level exception messages that can contain input.
- [x] **Step 4: Verify on API 26 and API 37.** Run credential/preferences/privacy tests, lint and assembly; inspect merged manifest and actual rule resources. Record a synthetic backup/restore canary check on an available device when its transport permits it; otherwise record the unavailable transport precisely and retain OEM transfer verification as a release gate. Static rules alone are not evidence of every OEM's behavior.
- [x] **Step 5: Commit the privacy boundary.** Commit `feat: add private credentials and theme preferences` without actual provider keys or release material.

### Task 4: Connect Compose screens and foreground lifecycle

**Deliverable:** The user can complete the approved offline story flow with persistent state, accessible controls and actionable cancelled/failed/interrupted drafts.

| Files | Change |
| --- | --- |
| A/TavernDroidApplication.kt, AppContainer.kt, GenerationCoordinator.kt, MainActivity.kt, Navigation.kt | Create composition and foreground owner; modify the minimal Activity |
| A/characters/CharactersScreen.kt, CharactersViewModel.kt; A/stories/StoriesScreen.kt, StoryScreen.kt, StoryViewModel.kt | Create manual character/session/conversation flow |
| A/settings/SettingsScreen.kt; A/library/LibraryScreen.kt; A/theme/TavernTheme.kt | Create theme settings and deliberate Library empty state |
| app/src/test/kotlin/io/github/cepeterjob/taverndroid/GenerationCoordinatorTest.kt, StoryViewModelTest.kt | Test initialization, command admission, retry and observation without replay |
| app/src/androidTest/kotlin/io/github/cepeterjob/taverndroid/OfflineStoryFlowTest.kt, ForegroundLifecycleTest.kt, AccessibilityContractTest.kt | Test real navigation, Activity recreation, backgrounding and accessible input |

**Interfaces:** AppContainer(context: Context, provider: GenerationProvider, dispatcher: CoroutineDispatcher, clock: () -> Long) wires concrete adapters; production supplies the approved fake. GenerationCoordinator exposes suspend initialize(): Unit, suspend send(sessionId: String, expectedRevision: Long, clientNonce: String, text: String): Admission, suspend retry(operationId: String, clientNonce: String): Admission, suspend stop(): Unit, onForeground(): Unit, onBackground(): Unit, and drafts: StateFlow<DraftUpdate?>. Initialization recovers unfinished rows before Send/Retry can admit; only Admitted can launch the app-scoped job. Stop/background terminalizes the admitted operation even if its worker has not entered run yet; cancellation handling in the runner alone cannot cover that gap. Track foreground state across suspended admission and launch: a Stop or foreground epoch change during admission cancels its eventual attempt, even if the app foregrounds again before admission returns. Own admission-to-launch in the application scope so caller cancellation cannot abandon a committed attempt. StoryViewModel exposes immutable StoryUiState derived from repository/operation streams, and send(text), stop(), retry(operationId), discardDraft(operationId) commands. Stable client nonces belong to user commands, not Flow collection or recomposition.

- [x] **Step 1: Write failing UI/coordinator contracts.** Complete create-character/start-story/send/reopen and assert one greeting, one user turn and one assistant. Recomposition, navigating away/back and ActivityScenario.recreate retain operation ID and provider invocation count. Stop immediately after admission cancels a queued worker without requiring its coroutine to start. Pause a controllable store before admission returns, background then foreground the coordinator, release admission, and require Cancelled with zero provider invocations; repeat with Stop. Separately cancel only the caller during suspended admission while the app stays foreground: application ownership must finish admission and run once, with one user turn and one attempt. Genuine process backgrounding cancels and preserves a saved draft; returning shows explicit Retry only. Failed/Cancelled/Interrupted have the approved recovery actions; Completed has no uncertain-retry action. Test blank/over-limit input, TalkBack semantics, 48 dp targets, scaled text and keyboard/last-message reachability.

In GenerationCoordinatorTest, construct an in-memory GenerationStore test double with immediate suspending methods, a counting GenerationProvider and GenerationCoordinator using StandardTestDispatcher(testScheduler); initialize, call onForeground and seed a SessionSnapshot before the assertion. Implement the double from Task 2's contract. Do not advance its scheduler until after Stop; real Room race coverage belongs to GenerationStoreTest.

```kotlin
@Test fun stopBeforeWorkerStartsTerminalizesAdmission() = runTest {
    val admitted = coordinator.send(session.identity.sessionId,
        session.identity.revision, "queued-send", "Hello") as Admission.Admitted
    coordinator.stop()
    advanceUntilIdle()
    val operation = store.observeLatestOperation(session.identity.sessionId).first()!!
    assertEquals(admitted.snapshot.operationId, operation.snapshot.operationId)
    assertEquals(OpState.Cancelled, operation.state)
    assertEquals(0, countingProvider.invocations)
}
```

For suspended admission, the test double exposes admissionStarted: CompletableDeferred<Unit> and releaseAdmission: CompletableDeferred<Unit>; it signals the first after durable admission and awaits the second before returning Admission.Admitted. All other methods remain immediate.

```kotlin
@Test fun backgroundDuringAdmissionPreventsLaterLaunch() = runTest {
    val send = async { coordinator.send(session.identity.sessionId,
        session.identity.revision, "suspended-send", "Hello") }
    suspendedStore.admissionStarted.await()
    coordinator.onBackground()
    coordinator.onForeground()
    suspendedStore.releaseAdmission.complete(Unit)
    assertTrue(send.await() is Admission.Admitted)
    advanceUntilIdle()
    assertEquals(OpState.Cancelled,
        suspendedStore.observeLatestOperation(session.identity.sessionId).first()!!.state)
    assertEquals(0, countingProvider.invocations)
}
```

- [x] **Step 2: Run and verify red behavior.** Run targeted app JVM tests and connected Android tests. Distinguish Activity recreation from process death; the latter belongs to Task 5's host scenario.
- [x] **Step 3: Implement screens and application ownership.** Connect ProcessLifecycleOwner ON_START/ON_STOP to foreground/background ownership; Android's delayed process events avoid cancelling on configuration changes. Capture cancellation/foreground identity before admission and recheck after its suspension before launching; mark eventual admitted work Cancelled when that identity changed. Observe state with lifecycle-aware collection, not a per-screen provider request. Serialize command launch at the coordinator and retain storage admission as the authority. Use Stories/Characters navigation, a full-screen story, Library empty state and Settings theme/fake-provider explanation. Render safe plain text; expose no credential entry, imports or advanced story controls. Own IME/system insets once, and keep draft separate from canonical dialogue.
- [x] **Step 4: Verify actual Android behavior.** Run app tests on both API levels, plus engine/provider/storage tests, lint, formatting and debug assembly. Manually check TalkBack, font scale 1.0 and 2.0, light/dark themes, gesture/three-button navigation and IME input reachability on supported devices. Record exact devices/results, not a visual-parity claim from JVM tests.
- [x] **Step 5: Commit the usable offline flow.** Commit `feat: connect offline stories and lifecycle recovery` and update README with the actual debug build behavior.

### Task 5: Deliver reproducible checks and interruption evidence

**Deliverable:** Credential-free CI builds an installable debug APK, runs contract checks and publishes validation reports; host-driven process death proves restart behavior on synthetic app data.

| Files | Change |
| --- | --- |
| .github/workflows/android.yml; tools/verify-wrapper.ps1; tools/verify-process-recovery.ps1 | Create pinned CI and host checks |
| app/src/androidTest/kotlin/io/github/cepeterjob/taverndroid/recovery/RecoverySeedTest.kt, RecoveryVerifyTest.kt | Create test-only checkpoint/kill/reopen fixtures |
| docs/development/p1-validation.md; README.md; docs/provenance/android-dependencies.md | Create actual evidence and shipped dependency/notice inventory; modify status only after checks |
| dependency lockfiles, storage/schemas/ | Verify outputs already introduced by owning tasks |

**Interfaces:** verify-wrapper.ps1 accepts -Root (defaults to the repository root) and exits nonzero on wrapper/JAR/version/digest mismatch. verify-process-recovery.ps1 accepts DeviceSerial and the debug APK/test APK paths; it operates only on io.github.cepeterjob.taverndroid.debug with synthetic data. RecoverySeedTest starts a controlled stream and writes a checkpoint-ready marker after a durable draft; host kills that process, relaunches, and RecoveryVerifyTest asserts Interrupted plus preserved user/draft and no automatic completed reply. A second seed covers durable completion before acknowledgment loss; verification requires exactly one assistant and Completed. The script treats only its intentional process termination as expected, never arbitrary test failures.

- [x] **Step 1: Add failing delivery checks.** Prove wrapper hash mismatch is rejected using a temporary copy, and host scenarios fail if recovery replays or duplicates output. Define CI groups for JVM tests/formatting, lint/assembly, Room/UI instrumentation on API 26 and 37, and Gitleaks history scanning with redacted output. No real provider or release secrets are available to PR jobs.

Prepare $testRoot with only copied wrapper files, replace its wrapper JAR with synthetic bytes, then assert the independent verifier rejects that root. The real repository wrapper is untouched.

```powershell
[IO.File]::WriteAllBytes((Join-Path $testRoot 'gradle\wrapper\gradle-wrapper.jar'), [byte[]](0, 1, 2))
& pwsh -NoProfile -File .\tools\verify-wrapper.ps1 -Root $testRoot
if ($LASTEXITCODE -eq 0) { throw 'Tampered wrapper was accepted' }
```

- [x] **Step 2: Run checks and establish red evidence.** A deliberately mismatched wrapper copy fails; an intentionally disabled recovery implementation fails the host contract. Do not count missing KVM/SDK as a behavior regression. Restore the implementation before normal verification.
- [x] **Step 3: Implement pinned delivery.** Use ubuntu-24.04, Temurin 25.0.3 and SDK packages 37.0/Build Tools 36.0.0. Pin the action commits in the integrity table. Use read-only contents permission, pull_request/push events, no pull_request_target, no release signing material and read-only Gradle caches on PRs. Verify the wrapper before running it. Download Gitleaks 8.30.1 from its official release, verify the archive hash, run `gitleaks git --redact --exit-code 1`, and fail on findings/errors. Boot authorized API 26 and API 37 emulator jobs using SDK tools, or run the same matrix locally if CI virtualization is unavailable and document that gap. Upload debug APK, test XML/HTML and lint reports; do not upload databases, drafts or credentials. Inventory actual resolved runtime artifacts/licenses and ship applicable notices.
- [x] **Step 4: Run the final evidence matrix.** Execute the commands below, the host abrupt-death/relaunch scenarios, and the airplane-mode offline flow. Record actual task names, test counts, APK SHA256, source revision, OS/device versions and results in p1-validation.md. Verify the merged manifest has no Internet permission, Room schema is exported and no destructive fallback exists. Do not claim physical startup/PSS benchmarks while the reference phone is unselected.
- [x] **Step 5: Commit and hand off the reviewed APK evidence.** Commit `ci: verify native debug APK and interruption contracts`; obtain a whole-branch review and resolve actionable findings before publishing a PR or APK. Update roadmap evidence only when authorized; do not check P0/P1 complete by inference.

## Verification commands and expected outcomes

These are planned conventional tasks, not tasks discovered in an existing build. Execution first runs `.\gradlew.bat projects` and `.\gradlew.bat :app:tasks --all`; correct the commands to actual generated names and record them. Do not use testAllSuites, which requires a later preview AGP, or clean as a default prerequisite.

```powershell
.\gradlew.bat :engine:test :providers:test :storage:testDebugUnitTest :app:testDebugUnitTest ktlintCheck
.\gradlew.bat :storage:lintDebug :app:lintDebug :app:assembleDebug
.\gradlew.bat :storage:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Each group must exit 0 with the expected reports and APK. Run connected tests once per API 26/API 37 target, with the intended device selected; do not let an unrelated connected phone determine the result. Expected APK path: app/build/outputs/apk/debug/app-debug.apk. Local scripts must inspect $LASTEXITCODE so a later successful shell statement cannot mask a failing Gradle or ADB command.

The host recovery check kills only the synthetic debug app process after observing the checkpoint marker; it is not an Activity recreation test or a graceful background cancellation test. Host controls relaunch through ADB and queries the subsequent test report. Instrumentation inside the killed process cannot verify its own death. Deadline all waits and preserve ordinary test failures.

Reference guidance: [Android command-line tests](https://developer.android.com/studio/test/command-line), [Room tests](https://developer.android.com/training/data-storage/room/testing-db), [ProcessLifecycleOwner](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner), [Keystore](https://developer.android.com/privacy-and-security/keystore), [backup controls](https://developer.android.com/identity/data/autobackup), [Gradle wrapper](https://docs.gradle.org/9.3.1/userguide/gradle_wrapper.html).

## Review and execution handoff

Plan approval and Native execution were confirmed by the maintainer in chat on 2026-10-11. The five tasks share the operation/storage interfaces closely; one implementer integrates them while independent investigations and final review remain useful. The maintainer's standing request for independent investigations still applies.

Physical reference selection is still open: record the actual ARM64 phone model, Android build and RAM, and whether it meets or intentionally differs from the 4 GB baseline. This does not invalidate the approved API floor or prevent preparation of the emulator-tested P1 work, but P0 and physical release benchmarks cannot be marked complete without that decision.
