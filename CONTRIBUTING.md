# Contributing to TavernDroid

TavernDroid is a standalone Android application. Changes must preserve local ownership of content and stories and the absence of a required server. P0 proposals are under review; application scaffolding begins after the written specification and implementation plan are approved.

## Before a change

1. Select one phase and link its requirement in [issue 1](https://github.com/cepeter-job/TavernDroid/issues/1).
2. For a substantial change, review a written specification and then a written implementation plan.
3. Identify the public behavior and automated tests that will establish acceptance.
4. Record the pinned upstream source and applicable notices for reused material.
5. Implement a focused change and provide actual test results in its pull request.

Do not include private stories, databases, provider credentials, signing keys, or environment files. Use synthetic examples. The proposed project license is GPL-3.0-only; do not port upstream material until the maintainer approves and installs the project license.

## Design and tests

Use constructor injection, readable Kotlin, and the four module boundaries in the specification. Keep business behavior out of composables and SQL out of the engine. Add tests for significant logic at its public boundary. Android lifecycle, Keystore, storage, and screen geometry need Android tests; a JVM test alone cannot establish their behavior.

Use the checked-in Gradle wrapper once P1 creates it. Every pull request must identify the module and variant checked, commands run, results, and any unavailable device check. Normal PR checks must run without paid provider credentials.

## Reviews and releases

Keep changes local until their publication is requested. Release signing material must stay outside Git and untrusted PR workflows. Claimed compatibility requires a fixture test; claimed device performance requires measurements on the recorded physical device. Preserve license and attribution notices on every adaptation.
