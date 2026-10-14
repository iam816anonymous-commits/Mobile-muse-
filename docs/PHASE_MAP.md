# PHASE_MAP.md — LocalAgent Authoritative 23-Phase Roadmap

This document defines the authoritative 23-phase implementation sequence (Phases 0 through 22) for **LocalAgent**. All phase work is governed by [`PHASE_PLAN.md`](../PHASE_PLAN.md).

---

## Authoritative Phase Roadmap

| Phase | Phase Name | Status | Primary Deliverables / Capabilities |
|---|---|---|---|
| **Phase 0** | Specification & Planning | COMPLETED | Architecture specifications & core governance rules |
| **Phase 1** | Foundation & Domain Core | COMPLETED | `NormalizedCommand`, `CapabilityRegistry`, `ActionPolicyEngine`, `GoalDispatcher` |
| **Phase 2** | Persistent Storage & Unified Logging | COMPLETED | Unified `agent.db` schema, `EventLogger`, `DurableMemoryStorageManager` |
| **Phase 3** | Permission & Capability Manager | COMPLETED | `PermissionManager`, in-app Permission Center, passive degradation |
| **Phase 4** | Accessibility Service Foundation | COMPLETED | `AgentAccessibilityService` binding & passive lifecycle monitor |
| **Phase 5** | Observation & Snapshot Engine | COMPLETED | Single-root snapshot extraction, `NodeIdentityConfidence`, `SnapshotDiffEngine` |
| **Phase 6** | Universal Action Engine | COMPLETED | Multi-strategy `TargetResolver`, `ActionableAncestorResolver`, `LiveTargetResolver` |
| **Phase 7** | Global & UI Action Execution | COMPLETED | `GlobalActionExecutor`, `UiActionExecutor`, target/navigation verification strategies |
| **Phase 8** | Application Control Engine | COMPLETED | `AppResolver`, `AppLauncher`, `LaunchVerifier`, launcher alias resolution |
| **Phase 9** | Command Console UI | PLANNED | Thin-client Console Activity & real-time log feed viewer |
| **Phase 10** | Automated Test Center | PLANNED | In-app contract suite runner & cross-channel parity runner |
| **Phase 11** | Movable Overlay Surface | PLANNED | `TYPE_APPLICATION_OVERLAY` floating console & action overlay |
| **Phase 12** | Hardware & System Controls | PLANNED | Volume, Brightness, Flashlight, Media, and Wi-Fi system controllers |
| **Phase 13** | Speech STT / TTS Subsystem | PLANNED | Multilingual SpeechRecognizer, TextToSpeech, transcript normalization |
| **Phase 14** | Workflow & Automation Engine | PLANNED | Multi-step workflow execution engine, retry policies, step verification |
| **Phase 15** | Episodic Memory & Learning | PLANNED | Procedural workflow learning, episodic task store, target re-validation |
| **Phase 16** | Browser Research Foundation | PLANNED | Web page observation engine, `<untrusted_external_content>` isolation |
| **Phase 17** | External Knowledge Ingestion | PLANNED | SAF document picker, `ChatExportParser`, SHA-256 provenance store |
| **Phase 18** | Structured Problem Solver | PLANNED | In-memory CSP/Sudoku solver, visual grid extraction, form field filler |
| **Phase 19** | Trip & General Research Engine | PLANNED | Multi-step travel/research goal decomposer, structured itinerary generator |
| **Phase 20** | On-Device / Cloud AI Planner | PLANNED | Pluggable LLM planner adapters, structured JSON plan output validator |
| **Phase 21** | Resource Hardening & Recovery | PLANNED | Low-RAM LMK process death recovery, cache eviction, threshold monitor |
| **Phase 22** | Master Device Certification | PLANNED | Physical API level 27 hardware certification suite & master report |
