# PHASE SCOPE GOVERNANCE RULE — LocalAgent Roadmap Alignment

## Mandatory Phase Alignment Rule

All development, code modifications, test additions, and architectural refactoring in **LocalAgent** are strictly governed by the authoritative 23-Phase Roadmap defined in [`PHASE_PLAN.md`](../PHASE_PLAN.md) and [`docs/PHASE_MAP.md`](PHASE_MAP.md).

### Rules of Scope Enforcement:
1. **Strict Sequential Phase Dependencies:** Developers and automated agents MUST complete lower-numbered phases before implementing higher-numbered phases.
2. **Explicit Phase Boundaries:** Functionality from Phase 9 or later (such as Command Console UI, In-App Test Center, Overlay Surfaces, Speech STT/TTS, Procedural Learning, Browser Ingestion, or LLM AI Planning) MUST NOT be pulled forward into Phase 0 through Phase 8 implementations.
3. **Phase Alignment Check Gate:** Every major implementation step or architectural change requires an explicit **Phase Alignment Check** verifying that all proposed changes fall strictly within the authorized phase boundaries.
4. **Governance Document Integrity:** `PHASE_PLAN.md` and `docs/PHASE_MAP.md` serve as the single source of truth for phase numbers, names, deliverables, test requirements, and exit criteria.
