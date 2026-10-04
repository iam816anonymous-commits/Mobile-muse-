# PHASE_PLAN.md — LocalAgent Implementation Roadmap & Phase Dependencies

## 1. Overview & Build Philosophy

The implementation of **LocalAgent** is organized into **23 distinct, strictly ordered phases** (Phase 0 through Phase 22).

### Core Rules of Progression
1. **Incremental Proof:** Each phase must be fully built, unit tested, contract tested, instrumentation tested, and verified on real device hardware before progressing to the subsequent phase.
2. **Cross-Phase Low-RAM Enforcement:** Low-RAM and low-resource constraints are enforced **from Phase 1 onward**, not deferred to a late optimization phase.
3. **No Skipping Steps:** Higher-level capabilities (e.g., Overlay, Voice, AI Reasoning, Solvers) must never be built before their foundational execution, logging, and permission contracts are proven.
4. **Definition of Done Enforced:** A phase is complete **only** when all unit tests, contract tests, and real-device test execution criteria specified in `DEFINITION_OF_DONE.md` are met.

---

## 2. Master Implementation Phase Table

| Phase | Phase Name | Primary Goal | Hard Dependencies | Deliverable / Verification |
|---|---|---|---|---|
| **Phase 0** | Specification & Planning | Freeze core specs, contracts, matrices, and test strategies. | None | 16 Spec MD files in Repo Root |
| **Phase 1** | Foundation & Domain Core | Build data models, `NormalizedCommand`, `CapabilityRegistry`, `ActionPolicyEngine`, and state machine. | Phase 0 | Domain Core Module & Unit Tests |
| **Phase 2** | Persistent Storage & Logging | Build unified SQLite `agent.db`, WAL/SHM manager, structured `EventLogger`. | Phase 1 | Event Persistence & WAL Accounting Tests |
| **Phase 3** | Permission & Capability Manager | Build runtime & special permission manager, API compatibility adapters (API 27–36+). | Phase 1, 2 | Permission Center UI & Diagnostics |
| **Phase 4** | Accessibility Service Foundation | Implement `AgentAccessibilityService`, passive service lifecycle, robust binding recovery. | Phase 2, 3 | A11y Service Binding & Connection Tests |
| **Phase 5** | Observation & Snapshot Engine | Single-root UI scanning, node identity matching, primitive extraction, node recycling. | Phase 4 | UI Snapshot Generator & Node Identity Tests |
| **Phase 6** | Universal Action Engine | Actionable ancestor traversal, live target resolution, action contracts. | Phase 5 | Target Resolver & Ancestor Traversal Tests |
| **Phase 7** | Global & UI Action Execution | Execute BACK, HOME, RECENTS, CLICK, LONG_CLICK, INPUT, SCROLL with verification strategies. | Phase 5, 6 | Action Execution & Action-Specific Diffs |
| **Phase 8** | Application Control Engine | Build `AppResolver`, `AppLauncher`, activity resolution, foreground verification. | Phase 6, 7 | App Launch & Foreground Verification |
| **Phase 9** | Command Console UI | Thin-client Console UI submitting commands to universal `GoalDispatcher`. | Phase 1, 7, 8 | Console Execution & Real-Time Logs |
| **Phase 10** | Automated Test Center | In-app test runner executing contract & parity tests without physical device. | Phase 1, 9 | Test Center Diagnostic UI & Contract Suite |
| **Phase 11** | Movable Overlay Surface | Implement `TYPE_APPLICATION_OVERLAY` floating control routing to `GoalDispatcher`. | Phase 3, 9, 10 | Movable Overlay & Action Parity |
| **Phase 12** | Hardware & System Controls | Volume, Brightness, Flashlight, Media Controls with API tier & risk checks. | Phase 3, 7 | Hardware Control Matrix Verification |
| **Phase 13** | Speech STT / TTS Subsystem | API 27 STT/TTS providers, multilingual support (EN, TE, KN, HI), idle destruction. | Phase 1, 9 | Speech Command Input & TTS Feedback |
| **Phase 14** | Workflow & Automation Engine | Triggers, conditions, sequential actions, retry policies, and timeouts. | Phase 7, 8, 9 | Deterministic Workflow Execution |
| **Phase 15** | Episodic Memory & Learning | Persistent execution history, procedural workflow learning, revalidation. | Phase 2, 14 | Workflow Learning & Storage Tests |
| **Phase 16** | Browser Research Foundation | Browser page navigation, web element observation, injection safeguards. | Phase 7, 8 | Web Observation & Injection Defense Tests |
| **Phase 17** | External Knowledge Ingestion | Import pipeline for ChatGPT/Gemini exports, user documents, provenance hash. | Phase 15, 16 | Untrusted Knowledge Importer Tests |
| **Phase 18** | Structured Problem Solver | Grid/board observation, state models, deterministic solver (Sudoku, form solver). | Phase 7, 15 | Solver Accuracy & Execution Tests |
| **Phase 19** | Trip & General Research Engine | Multi-step goal research, travel option extraction, itinerary planning with citations. | Phase 16, 17 | Research Planner & Report Generator |
| **Phase 20** | On-Device / Cloud AI Planner | Optional LLM intent classification & plan outputting `NormalizedCommand`s. | Phase 1, 14, 19 | AI Intent to Execution Pipeline |
| **Phase 21** | Resource Hardening & Recovery | Process recovery after LMK termination, cache eviction, memory stress tests. | Phase 1–20 | Low-RAM Survival & LMK Recovery Tests |
| **Phase 22** | Master Device Certification | Execute master automated test suite on physical Android 8.1 (API 27) device. | Phase 1–21 | Master Certification Audit Report |

---

## 3. Phase Dependency Graph

```text
Phase 0 (Specs)
  └── Phase 1 (Core Domain)
        ├── Phase 2 (Storage & Logging)
        │     └── Phase 4 (Accessibility Service)
        │           ├── Phase 5 (Observation Engine)
        │           │     ├── Phase 6 (Universal Action Engine)
        │           │     │     └── Phase 7 (Global & UI Actions)
        │           │     │           ├── Phase 8 (App Control)
        │           │     │           │     └── Phase 9 (Console UI)
        │           │     │           │           ├── Phase 10 (Automated Test Center)
        │           │     │           │           │     └── Phase 11 (Movable Overlay)
        │           │     │           │           └── Phase 13 (Speech STT/TTS)
        │           │     │           ├── Phase 12 (Hardware Controls)
        │           │     │           └── Phase 14 (Workflow Engine)
        │           │     │                 ├── Phase 15 (Episodic Memory)
        │           │     │                 │     ├── Phase 16 (Browser Research)
        │           │     │                 │     │     ├── Phase 17 (External Knowledge)
        │           │     │                 │     │     └── Phase 19 (Trip Research Engine)
        │           │     │                 │     └── Phase 18 (Structured Problem Solver)
        │           │     │                 └─────────────────┬──────────────────┘
        │           │     │                                   ▼
        │           │     │                             Phase 20 (AI Planner)
        │           │     │                                   │
        │           │     │                                   ▼
        │           │     │                             Phase 21 (Resource Hardening)
        │           │     │                                   │
        │           │     │                                   ▼
        │           │     │                             Phase 22 (Master Certification)
        │           └─────┴───────────────────────────────────┘
        └── Phase 3 (Permission Manager)
```
