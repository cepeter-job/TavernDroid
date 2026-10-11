# Native P1 review and decisions

A fresh-context reviewer examined the whole branch in two passes:
`2dbad3421341bf1c61f690c44c7ec8679022d405..63c51619b959e096350232c9171b0eb2c2cffa0d`.
The reviewer changed no files and did not rerun tests. The initial verdict was
"With fixes": three Important findings, no Critical or Minor findings.
The implementer resolved all three in one test-driven pass in
`779dfa83a0aa046c59e8cc4c72ef000f558f549f`. Acceptance is supported by the failing
regressions followed by the complete passing matrix, rather than a second review.
No push, merge, PR or release is implied.

## Findings resolved

| Finding | Regression and final behavior |
| --- | --- |
| Atomic credential reads bypassed interrupted-write recovery on API 26 | `interruptedReplacementKeepsLastCommittedCredential` and `legacyBackupWithoutBaseRecoversCommittedCredential` failed with CorruptCiphertext/Absent, then passed after reads used AtomicFile.readFully. The previous committed ciphertext survives incomplete replacement. |
| Restoring a pending character-save flag orphaned its owner | `restoringFormDoesNotKeepAnAbandonedSaveDisabled` failed on enabled-state assertion, then passed. Form input stays saveable; the pending flag is transient like its composition coroutine. |
| Start story and appearance-write exceptions escaped UI coroutines | Two `PersistenceCommandTest` failure/retry tests initially threw IOException, then passed. Start keeps the current screen and displays a retry message; appearance keeps the durable selection and displays its error. Two cancellation contracts continue to rethrow cancellation. |

The complete post-fix check passed: 34 JVM tests, 27 storage plus 9 app Android
tests on each API 26/37 emulator, and three abrupt-death verifications on each.
Two host-phase entry points are intentionally skipped in normal connected suites.
Wrapper rejection checks, formatting, lint, assembly, packaged notices and APK
privacy/schema checks passed. See [validation](p1-validation.md) for commands,
devices, APK hash and measured limitations.

The reviewer positively traced application-owned admission, foreground epochs,
transactional publication, retry retaining the user turn, stale checkpoint
rejection, first-terminal wins and restart without replay. The scripts preserve
test failures and distinguish intentional process death; CI follows the approved
integrity/permission policy. All 129 runtime coordinates match the notices.

## Rulings I made during implementation

These are the complete ledgered implementation decisions, in order. Costs state
what must change if the decision proves wrong; they are not additional promises.

| Decision | Reason | Cost if wrong |
| --- | --- | --- |
| Run shared stale-generation fixture in real Room Android tests, retaining engine resource | Publication authority is Room; platform JSONObject avoids an extra JVM dependency | Relocate a test asset; no runtime change |
| Omit invented cross-platform-transfer identifiers, retain nine legacy exclusions | No legitimate iOS counterpart; SDK37 FullBackup falls back to legacy rules for this mode | Add verified platform rules before a cross-platform release; OEM behavior remains unverified |
| Explicit adjustResize and clear focus when starting a story | API26 screenshot exposed whole-window panning hiding the greeting/composer | Adjust one manifest/focus decision |
| Dismiss IME before test commands and scroll recovery controls into view | External keyboard animation is outside Compose's clock | Add stricter platform animation synchronization |
| Activate functional Save/Send using their accessibility OnClick action | Repeated missed taps were at the IME animation boundary; separate geometry/manual checks remain | Add dedicated touch/animation release tests |

## Rulings on behaviors the reviewer set aside

Each declined-to-judge item was independently assessed by its effect on a user.

| Behavior | Decision and user effect | Cost if wrong |
| --- | --- | --- |
| Real providers, imports, edit/reset/branching and advanced rendering | Deferred by approved scope; users receive the documented offline fake flow | Design and test those features before advertising them |
| Liveness after direct SQL identity mutation in tests | No production mutation command exists; stale publication is protected | Future mutation commands must terminalize/invalidate active work |
| Physical performance, large histories, low memory, audible TalkBack, OEM transfer | Explicit unverified release gates; no emulator result is presented as physical proof | Obtain reference devices and measure before release |
| Hosted GitHub execution and exhaustive vendor Keystore invalidation | Local checks support only their stated boundaries | Run hosted CI and vendor checks before release |
| Complete tooling licensing and multi-process credential writers | Runtime notices are inventoried; one application process owns one credential adapter | Audit redistributed tooling or add cross-process coordination before enabling either |

Deferred minors: none.

## Remaining handoff boundary

The implementation branch remains local. The physical reference phone is still
unselected; no P0/P1 GitHub roadmap boxes were checked. Integration/publication
requires the maintainer's choice. The debug APK and synthetic validation evidence
can be reviewed without merging or publishing.
