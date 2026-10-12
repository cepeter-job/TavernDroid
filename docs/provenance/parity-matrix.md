# Upstream behavior parity matrix

Reference all paths below against [commit 38b17d10d7630baec9efe53320ca94f8ab122bda](https://github.com/cepeter-job/SillyTavern-Telegram-Bridge/tree/38b17d10d7630baec9efe53320ca94f8ab122bda). Port, redesign, defer and remove describe intended treatment, not implementation completion. No Android parity is claimed in P0.

The [dependency inventory](dependencies.md) records the six direct runtime constraints, all 42 locked distributions, and development/browser test tooling.

| Capability | Pinned source and representative test | Treatment and Android owner | Phase and target contract |
| --- | --- | --- | --- |
| Characters and personas | bridge/card_content.py, bridge/native_imports.py, bridge/persona_service.py; tests/test_persona_native_storage.py::test_explicit_persona_store_preserves_source_extension_and_bytes | Port behavior; :engine/:storage | Manual characters P1; supported imports/revisions P3 |
| Lore activation | bridge/world_storage.py, bridge/card_content.py; tests/test_world_management.py::test_upload_accepts_world_info_and_refuses_duplicate | Port documented subset; :engine/:storage | P3 basic keys/secondary keys/order; no claim of full ST lore semantics |
| Presets | bridge/generation_settings.py, bridge/generation_settings_repository.py, bridge/card_content.py; tests/test_system_prompt_catalog.py::test_full_export_is_not_a_native_catalogue | Port simple formats, redesign unsupported-field reporting; :engine | P3 native TXT/simple JSON; full prompts/prompt_order ST exports excluded until separately specified |
| Prompt order and final budgets | bridge/generation.py, bridge/card_content.py, bridge/context_compaction.py; tests/test_post_history_placement.py::test_card_tail_follows_history_and_native_policy_follows_card | Port deterministic contracts; :engine | P2 basic budget refusal; P3 ordering; P4 compaction only after continuity tests |
| Sessions and variants | bridge/session_service.py, bridge/session_repository.py, bridge/response_variants.py, bridge/variant_repository.py; tests/test_swipe_callback_safety.py::test_old_swipe_cannot_change_a_newer_transcript | Port identity/ownership, redesign UI; :engine/:storage | P1 session isolation; variants P3 |
| Edit and reset | bridge/edit_messages.py, bridge/regeneration.py; tests/test_reset_behavior.py::test_reset_clears_every_derived_narrative_table_and_keeps_preferences | Port invalidation, redesign lifecycle/delivery; :engine/:storage | P1 stale-result boundary; public edit/reset behaviors P3/P4 |
| Narrative choices | bridge/light_novel_format.py, bridge/light_novel_service.py, bridge/light_novel_turn.py; tests/test_light_novel_flow.py::test_queued_choice_from_before_reset_never_enters_new_story | Port shape/identity, redesign native actions; :engine/:app | P3 requested 3 or 4 distinct choices, each at most 160 characters; free text remains available |
| Narrative authority | bridge/narrative_policy.py, bridge/narrative_reconciliation.py; tests/test_narrative_acceptance_recovery.py::test_source_revision_change_suppresses_retry_or_rejects_its_publication | Port authority rules; :engine | P3 viewpoint/focus; P4/P5 derived-state publication guards |
| Memory coverage | bridge/memory_contracts.py, bridge/memory_draft_publish.py, bridge/memory_scope_runtime.py; tests/test_memory_complete_parts.py::test_malformed_part_never_advances_coverage | Port coverage/provenance; :engine/:storage | P4 accepted complete contiguous ranges only; invalid or partial parts never advance coverage |
| Reader scope and retrieval | bridge/memory_scope_store.py, bridge/memory_search_store.py; tests/test_story_memory_scope.py::test_legacy_unknown_provenance_is_not_character_authority | Port local authority, redesign indexing; :engine/:storage | P4 reader/branch isolation; Android-compatible lexical retrieval |
| Director | bridge/director_service.py, bridge/director_validation.py; tests/test_director_service.py::test_late_proposal_never_overwrites_newer_reality_or_manual_intent | Port structured contracts; :engine | P5 stale proposals rejected; bounded repairs and explicit routing |
| Ending and alternate branches | bridge/ending_service.py, bridge/epilogue_service.py, bridge/closed_session_guard.py, bridge/alternate_ending_runtime.py; tests/test_alternate_ending_recovery.py::test_restart_resumes_prepared_local_work_and_returns_same_target | Port terminal guards, redesign recovery; :engine/:storage | P5 one finale, retryable separate epilogue, independent alternate branch |
| SQLite migrations | bridge/migrations.py, bridge/schema.py; tests/test_migrations.py | Redesign native schema; :storage | P1 Room lineage begins at 1; future migrations transactional and history-preserving; do not copy upstream migration numbering |
| Provider transport | bridge/generation.py and provider contracts in docs/development/architecture.md | Redesign independent adapters; :providers | P2 NanoGPT and OpenRouter each require their own capability/stream/error tests |
| Telegram and server deployment | Telegram adapters, Mini App transport and Linux deployment in docs/development/architecture.md | Remove from Android runtime | No polling, web hosting, shared server files, systemd or VPS setup |
| Remote memory and rich media | bridge/memory_scope_runtime.py; requirements.txt | Defer optional services and speech/image stacks | P7 separate measured proposals; no prerequisite for P1/P2 |

## Prompt ordering to preserve in P3

The pinned initial system assembly orders card system/default, description, personality, scenario, example dialogue, session system, persona, knowledge boundary, scene, summary, memory/NPC/RAG policies, group rules, world info, and author note. Greeting is inserted only with empty history; conversation history follows.

If a card post-history instruction exists, a late system message contains the card tail followed by native narrative, user-agency, state and output policies. The explicit language constraint follows. The current user message remains last; its scoped memory/NPC/simulation/episodic blocks precede user text and RAG references follow it. Without a card tail, native runtime policy belongs in the initial system. These advanced paths are not P1 scope.

## Synthetic fixture catalog

| Fixture | Invariant and planned consumer |
| --- | --- |
| [stale-generation.json](fixtures/stale-generation.json) | P1 engine/storage contract: a reset changes incarnation; late output cannot publish |
| [post-history-order.json](fixtures/post-history-order.json) | P3 prompt-order adapter: history precedes card tail/native policy, current user remains last |
| [memory-partial.json](fixtures/memory-partial.json) | P4 memory contract: incomplete parts of one canonical source row cannot advance coverage through that row; earlier complete rows remain publishable |
| [invalid-choices.json](fixtures/invalid-choices.json) | P3 choice contract: invalid choices remain unavailable while the saved story survives |

Each fixture records synthetic authorship and the source behavior it represents. The fixture schema is a documentation contract; no production parser or automated test exists yet. Later phase tests must consume or adapt these inputs and verify exact outputs. Keep fixture content public and synthetic; do not use upstream chat history or live databases.
