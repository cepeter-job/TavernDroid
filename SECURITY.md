# TavernDroid security policy

TavernDroid is awaiting implementation-plan review and has no released APK or supported release branch. Security requirements apply before real provider credentials or private story data enter an implementation.

## Reporting a vulnerability

Use the repository's GitHub private vulnerability reporting channel when enabled. If no private channel is available, open a minimal issue titled "Private security contact requested" without exploit details, credentials, or private transcripts. The maintainer must establish and verify a private reporting channel before the direct-provider alpha.

Ordinary defects can be reported as public issues using synthetic examples. This project does not promise a response time or support period before a release policy exists.

## Data boundaries

Encrypt provider credentials with an Android Keystore protected key and keep ciphertext in app-private storage. Redact secrets and transcript content from diagnostics. Canonical stories live in Room; this alone is not database encryption or end-to-end encryption.

Disable unintended cloud backup and device transfer for credentials and private story data. A later explicit export or backup must exclude credentials and state its encryption and restore behavior. Imported cards, lore, and presets are untrusted data; never execute embedded scripts or tools.

The fake-provider P1 build must not request network access. Provider onboarding in P2 must explain that selected providers receive prompt content. Remote memory and synchronization remain disabled unless separately designed and explicitly enabled.
