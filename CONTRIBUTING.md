# Contributing to TavernDroid

TavernDroid is a standalone Android application. Changes must preserve local ownership of content and stories and the absence of a required server. The P0 specification and native P1 plan are approved. The offline debug implementation is available on the development branch; see [validation](docs/development/p1-validation.md) for measured checks and remaining release gates.

## Before a change

1. Select one phase and link its requirement in [issue 1](https://github.com/cepeter-job/TavernDroid/issues/1).
2. For a substantial change, review a written specification and then a written implementation plan.
3. Identify the public behavior and automated tests that will establish acceptance.
4. Record the pinned upstream source and applicable notices for reused material.
5. Implement a focused change and provide actual test results in its pull request.

Do not include private stories, databases, provider credentials, signing keys, or environment files. Use synthetic examples. Contributions are licensed under GPL-3.0-only; preserve provenance and applicable notices on upstream adaptations.

## Design and tests

Use constructor injection, readable Kotlin, and the four module boundaries in the specification. Keep business behavior out of composables and SQL out of the engine. Add tests for significant logic at its public boundary. Android lifecycle, Keystore, storage, and screen geometry need Android tests; a JVM test alone cannot establish their behavior.

Verify the checked-in Gradle wrapper with `pwsh -File tools/verify-wrapper.ps1` before running it. Every pull request must identify the module and variant checked, commands run, results, and any unavailable device check. Normal PR checks run without paid provider credentials.

## Reviews and releases

Keep changes local until their publication is requested. Release signing material must stay outside Git and untrusted PR workflows. Claimed compatibility requires a fixture test; claimed device performance requires measurements on the recorded physical device. Preserve license and attribution notices on every adaptation.
