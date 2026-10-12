# Licensing and provenance review

TavernDroid is licensed under GPL-3.0-only. The maintainer approved this choice with the P0 specification on 2026-10-11. The complete [LICENSE](../../LICENSE) is installed from the Free Software Foundation's [GPLv3 text](https://www.gnu.org/licenses/gpl-3.0.txt). Preserve the upstream grant and applicable notices when adapting material.

## Pinned upstream declaration

Repository: [SillyTavern Telegram Bridge](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge), commit `38b17d10d7630baec9efe53320ca94f8ab122bda`. Its [README](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge/blob/38b17d10d7630baec9efe53320ca94f8ab122bda/README.md) declares GNU General Public License v3.0 and its [LICENSE](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge/blob/38b17d10d7630baec9efe53320ca94f8ab122bda/LICENSE) contains the GPLv3 text.

Record GPL-3.0-only conservatively: the inspected project declaration does not contain a project-specific "or later" grant. The generic application example inside GPLv3 is not an independent project grant. The Free Software Foundation copyright in LICENSE identifies the license text, not the bridge's authorship. Preserve actual source copyright notices; do not invent an individual bridge author.

The local `D:\SillyTavern-Telegram-Bridge` checkout points to a different GitHub owner and does not contain the pinned object. The parity review uses public files at the pinned cepeter-job repository. Neither that local checkout nor its deployments were changed.

## Reuse ledger

| Upstream material | Evidence and handling |
| --- | --- |
| Bridge behavior and code | Retain repository identity, exact source revision/path, license and applicable notices; mark Kotlin adaptations and changes |
| bridge/humanize.py | Upstream notice identifies MIT inspiration from blader/humanizer, Copyright (c) 2025 Siqi Chen; retain applicable notice if reused |
| bridge/codex_auth.py and bridge/codex_transport.py | Upstream notice identifies MIT NousResearch/hermes-agent contracts, Copyright (c) 2025 Nous Research; these adapters are outside the initial Android scope |
| assets/character-ranks | Upstream documents user-supplied assets but supplies no separate asset license grant; do not import them without an explicit permitted basis |
| Android libraries and imported user content | Inventory the artifacts actually shipped and their licenses separately; Python dependency notices do not license Android replacements or character artwork |

Notice source: [THIRD_PARTY_NOTICES.md](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge/blob/38b17d10d7630baec9efe53320ca94f8ab122bda/THIRD_PARTY_NOTICES.md). No upstream code or artwork is copied in P0; the fixtures are hand-authored synthetic data. Rewriting code in Kotlin does not erase the need to review the applicable upstream grant.

## Before a port or release

1. Verify that the approved project license and complete text accompany the material.
2. Record the source commit, path, reused scope and actual notices for each port.
3. Retain the applicable license and copyright text and identify adaptations.
4. Review licenses and provenance of bundled dependencies and assets.
5. Ship corresponding source and notices as required by the approved licenses; verify the release package before distribution.

The maintainer owns the final licensing decision. Unresolved ownership or grants stop reuse of that material, not unrelated local design work.
