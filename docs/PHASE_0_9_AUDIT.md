# docs/PHASE_0_9_AUDIT.md — Phase 0.9 Architecture Consistency Audit & Freeze Assessment

## 1. Executive Summary

This document presents the comprehensive audit of the **Phase 0.9 Architecture Specification Package** for **LocalAgent**.

During this pass, every specification document was audited against Android 8.1 / API level 27 platform realities, low-RAM constraints, universal command execution requirements, security boundaries, and cross-module consistency.

### Final Readiness Assessment: **PHASE 0.9 READY FOR FREEZE**

The Phase 0.9 specification package is hereby **FROZEN** as the formal implementation contract for all future build phases.

---

## 2. Audit Scope & Document Inventory

The audit verified all 16 primary specification documents in the repository root plus supporting planning artifacts:

1. `ARCHITECTURE.md` — Core Architecture & Module Specifications
2. `PHASE_PLAN.md` — 23-Phase Implementation Roadmap & Phase Contracts
3. `CAPABILITY_MATRIX.md` — Universal Capability Matrix & `CapabilityRule` Contracts
4. `PERMISSION_MATRIX.md` — Permission Tiers & SAF Storage Protocol
5. `ACTION_CONTRACTS.md` — Target-Aware Action Contracts & `VerificationStrategy` Protocol
6. `OBSERVATION_MODEL.md` — Single-Root Observation Snapshot Generator & Composite Node Identity Model
7. `LOGGING_AND_AUDIT.md` — Structured Event Logging & Unified `agent.db` WAL Storage Accounting
8. `LOW_RAM_DESIGN.md` — Cross-Phase Low-RAM Architecture & Memory Threshold Profiles
9. `MEMORY_AND_LEARNING.md` — Memory Subsystem, `GoalState`, `TaskLifecycle` & `ExecutionLock`
10. `STT_TTS_ARCHITECTURE.md` — Multilingual Speech Engine & `LanguageEngineStatus` Capabilities
11. `EXTERNAL_KNOWLEDGE_ARCHITECTURE.md` — External Research, Chat Import & Prompt Injection Defenses
12. `HARDWARE_CAPABILITIES.md` — Hardware Control Matrix & Risk Policy Integration
13. `TESTING_STRATEGY.md` — 3-Level Testing Architecture & Certification Framework
14. `RESEARCH_SOURCES.md` — Research Sources & Architectural Inferences
15. `DEFINITION_OF_DONE.md` — Multi-Tiered Definition of Done & Prohibitions
16. `RISKS_AND_LIMITATIONS.md` — Architectural Risks & Platform Boundaries
17. `docs/PHASE_0_9_AUDIT.md` — This Audit & Freeze Assessment

---

## 3. Key Contradictions & Technical Flaws Resolved

### 3.1 Single-Root Snapshot Lifecycle & Native Memory Leak Prevention
- **Issue Discovered:** Early observation draft acquired `rootInActiveWindow`, recycled it, and then called `rootInActiveWindow` a second time during the same snapshot cycle.
- **Correction Applied:** `OBSERVATION_MODEL.md` explicitly mandates acquiring `rootInActiveWindow` **EXACTLY ONCE** per snapshot cycle. Node primitives are extracted into immutable data objects and every native `AccessibilityNodeInfo` reference is recycled in `finally` blocks.

### 3.2 Ephemeral Index vs Composite Stable Node Identity
- **Issue Discovered:** Early drafts used snapshot-local counters (`nodeId = counter++`) for cross-snapshot target matching, causing snapshot diffing to fail whenever UI structure shifted.
- **Correction Applied:** `OBSERVATION_MODEL.md` explicitly classifies `observationInstanceId` and `parentInstanceId` as **ephemeral, snapshot-local metadata ONLY**. Cross-snapshot target matching uses a composite `nodeIdentity` key and a formal `NodeIdentityConfidence` model (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`).

### 3.3 Target-Aware Action Verification
- **Issue Discovered:** Action verification relied on generic "any UI change = success", allowing unrelated background UI mutations (e.g., clock ticking) to trigger false positives.
- **Correction Applied:** `ACTION_CONTRACTS.md` formalized `VerificationStrategy` implementations (`TargetClickStrategy`, `ToggleVerificationStrategy`, `TextInputStrategy`, `ScrollVerificationStrategy`, `NavigationAwareBackStrategy`). Verified success is granted ONLY when target-specific UI diff criteria are satisfied.

### 3.4 Consolidated SQLite Storage & WAL Accounting
- **Issue Discovered:** Storage models proposed multiple separate SQLite databases (`workflows.db`, `episodic.db`, `knowledge.db`, `logs.db`) without accounting for SQLite Write-Ahead Logging (`.db-wal`) overhead.
- **Correction Applied:** `LOGGING_AND_AUDIT.md` consolidated all persistent data into a single `agent.db` SQLite database. Storage limits strictly account for `agent.db` + `agent.db-wal` + `agent.db-shm` files under a 30 MB maximum cap with automated `PRAGMA wal_checkpoint(TRUNCATE)` maintenance.

### 3.5 Storage Access Framework & Permission Corrections
- **Issue Discovered:** `PERMISSION_MATRIX.md` incorrectly required broad `READ_EXTERNAL_STORAGE` permissions for normal document importing and listed `<queries>` as a runtime permission.
- **Correction Applied:** `PERMISSION_MATRIX.md` corrected storage import to use Storage Access Framework (SAF) `Intent.ACTION_OPEN_DOCUMENT` (`content://` URIs), eliminating broad storage permission requirements. Configured `<queries>` as Manifest Configuration and `INTERNET` as Normal Manifest Permission.

### 3.6 Passive Degradation Semantics
- **Issue Discovered:** Permissions matrix claimed Accessibility unbinding forced the entire app into "mock mode".
- **Correction Applied:** Clarified passive degradation rules across `PERMISSION_MATRIX.md` and `RISKS_AND_LIMITATIONS.md`. If Accessibility is unbound, non-accessibility modules (Console UI, Movable Overlay surface, Voice STT, Settings UI, EventLogger, Solvers, Research Engine) remain 100% operational. Accessibility actions return `ACCESSIBILITY_UNAVAILABLE` safely.

### 3.7 Concurrency Control & Single-Threaded Execution Lock
- **Issue Discovered:** Simultaneous input channels (Console, Overlay, Voice, AI) could dispatch competing device actions concurrently, causing race conditions.
- **Correction Applied:** `MEMORY_AND_LEARNING.md` introduced the `ExecutionLock` policy. All device actions pass through `GoalDispatcher`'s priority queue; only ONE foreground device-control transaction manipulates the Accessibility execution channel at a time.

### 3.8 Multi-Tiered Testing Pyramid
- **Issue Discovered:** Test strategies lacked clean separation between in-memory contract tests and real-device framework execution.
- **Correction Applied:** `TESTING_STRATEGY.md` re-architected testing into three explicit levels: LEVEL 1 (Contract/Unit), LEVEL 2 (Device Self Tests), and LEVEL 3 (Cross-App E2E). Explicitly mandated that `CONTRACT TEST PASS != DEVICE EXECUTION PASS`.

---

## 4. Final System Architecture Overview

```text
                     ┌────────────────────────────────────────────────────────┐
                     │                     INPUT SURFACES                     │
                     │  Console  │  Movable Overlay  │  Voice  │  Test Harness│
                     │  Future AI Planner   │   Future Browser Learning       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               INPUT NORMALIZATION LAYER                │
                     │   Command / Intent Normalizer → NormalizedCommand       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │          UNIVERSAL CONCURRENCY DISPATCHER              │
                     │  GoalDispatcher (Priority Queue + ExecutionLock)       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │         CAPABILITY REGISTRY & POLICY ENGINE            │
                     │  CapabilityRegistry ──► ActionPolicyEngine             │
                     │  (Risk Levels: LOW, MEDIUM, HIGH, CRITICAL)             │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │                  TARGET RESOLVER                       │
                     │  Single-Root Live Acquisition ──► Ancestor Traversal  │
                     │  (Clickable / Long-Clickable / Scrollable / Editable)  │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │                  ACTION EXECUTORS                      │
                     │ Accessibility Engine │ System APIs │ Hardware Controls │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               OBSERVATION & VERIFICATION               │
                     │  Single Root Snapshot Capture ──► VerificationStrategy │
                     │  (SUCCESS_VERIFIED / DISPATCHED_UNVERIFIED / FAILED)   │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               PERSISTENCE & AUDIT LOGGING              │
                     │  Structured EventLogger ──► Unified agent.db           │
                     │  (Episodic, Procedural, Semantic, Knowledge Tables)    │
                     └────────────────────────────────────────────────────────┘
```

---

## 5. Architectural Decision Records (ADRs) Summary

- **ADR-01: Universal Execution Pipeline:** All input channels MUST converge into `NormalizedCommand` objects processed by `GoalDispatcher`. No input surface may implement isolated action execution logic.
- **ADR-02: API 27 Baseline Compatibility:** Android 8.1 (API 27) is the hard minimum target baseline. Higher API features (API 28–36+) must be accessed via compatibility adapters.
- **ADR-03: Single-Root Snapshot Recycling:** Accessibility trees are queried ONCE per snapshot and every node primitive is recycled immediately in `finally` blocks.
- **ADR-04: Untrusted External Content Boundary:** Web pages, browser text, chat exports, and AI responses are strictly classified as `<untrusted_external_content>` data and MUST NEVER be converted directly into system commands without passing `ActionPolicyEngine` checks.
- **ADR-05: Consolidated Database Storage:** All local persistence is stored in tables inside a single `agent.db` SQLite database with WAL file size accounting under a 30 MB total cap.

---

## 6. Freeze Declaration

The Phase 0.9 architecture and specification package is complete, internally consistent, technically validated against Android API 27 baseline constraints, and **FREEZE READY**.

**Implementation Phase 1 (Core Foundation Implementation) is authorized to begin.**
