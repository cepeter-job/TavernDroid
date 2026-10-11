# Upstream dependency inventory

Inventory the public manifests at commit `38b17d10d7630baec9efe53320ca94f8ab122bda`. None of these Python distributions becomes an Android runtime dependency. The Kotlin port selects and reviews its own libraries; this inventory explains which server capabilities are replaced or deferred.

## Direct runtime requirements

| Requirement from requirements.txt | Upstream function | Android treatment |
| --- | --- | --- |
| defusedxml >=0.7.1,<1 | Safe XML parsing | Remove Python runtime; evaluate bounded format parsers only when imports require them |
| faster-whisper >=1.2.1,<2 | Speech transcription | Defer voice to P7 |
| pypdf >=6.19.0,<7 | PDF processing | No P1 import requirement; defer document import |
| hindsight-client >=0.10.2,<0.11 | External memory service | Defer to explicit optional P7 adapter |
| PyYAML >=6,<7 | YAML parsing/configuration | Remove server configuration; native settings and explicitly supported content formats |
| aiohttp >=3.14.3,<4 | Async HTTP/client infrastructure | Redesign Kotlin provider transport in P2 |

[requirements.txt](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge/blob/38b17d10d7630baec9efe53320ca94f8ab122bda/requirements.txt) defines the six direct constraints. [upstream-runtime-lock.csv](upstream-runtime-lock.csv) records all 42 exact distributions from [requirements.lock](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge/blob/38b17d10d7630baec9efe53320ca94f8ab122bda/requirements.lock), including transitive speech, HTTP and validation packages. It is reference data, not an installation manifest.

## Development and browser test dependencies

| Manifest | Packages and pins | Android treatment |
| --- | --- | --- |
| requirements-dev.txt | mypy 2.4.0, pytest 9.1.1, pytest-xdist 3.8.0, ruff 0.16.10, pytest-cov 7.1.0, coverage 7.16.2, packaging 26.3, pip-audit 2.10.1, uv 0.12.22 | Optional isolated upstream reference work only; use Kotlin/Android tests for shipped behavior |
| tests/miniapp-ui/package.json | jsdom 30.1.1 | Remove Mini App UI tests from native build; replace with Compose tests |
| tests/miniapp-browser/package.json | @playwright/test 1.63.0 | Remove browser delivery tests from native build; replace with Android lifecycle/instrumentation tests |
| pyproject.toml | Tool configuration | No Python packaging or runtime metadata is imported |

Do not install the upstream Python or Node test stacks as TavernDroid prerequisites. Keep optional upstream execution in a separate checkout with synthetic data and separate caches.

## Native dependency selection gate

P1 requires Compose UI, Room and its supported processor, coroutines, DataStore, lifecycle/navigation and test libraries. The toolchain compiler/BOM pins are already proposed in [Windows setup](../development/windows-setup.md). Before scaffolding, the implementation plan must record exact artifact versions and Kotlin/processor compatibility, license identifiers and notices, repository sources, and wrapper/CI integrity checks. HTTP transport is P2; speech, retrieval services and on-device inference are not P1 dependencies.
