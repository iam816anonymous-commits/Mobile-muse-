# README.md — LocalAgent Repository Index & Architecture Overview

## Overview

**LocalAgent** is a low-RAM, offline-first Android device agent capable of observing, reasoning about, and safely executing actions on Android devices. It prioritizes **deterministic automation first**, with optional AI learning, research, and planning layers sitting above it.

This repository is currently in **Phase 3.1 Completion** status. All Phase 0/0.9 specifications are frozen, Phase 1 domain core, Phase 2 persistent storage, and Phase 3.1 read-only accessibility observation foundation are implemented and verified.

---

## Governance & Phase Alignment Rule

All development and automated agent implementations in LocalAgent are governed by the **Permanent Phase Scope Governance Rule** defined in [`docs/PHASE_SCOPE_GOVERNANCE_RULE.md`](docs/PHASE_SCOPE_GOVERNANCE_RULE.md).

Developers and AI agents MUST perform an explicit **Phase Alignment Check** before implementing any requirement, test, UI screen, or architectural change. Functionality from a future phase must NEVER be silently pulled forward or merged into the current phase without explicit project-owner approval.

---

## Phase 0.9 & Phase Specification Document Index

All architectural specifications, phase governance rules, and audit reports are indexed below:

1. **`ARCHITECTURE.md`** — Core Architecture, Unified Pipeline & Directory Structure
2. **`PHASE_PLAN.md`** — 23-Phase Implementation Roadmap & Phase Contracts (Phases 0 – 22)
3. **`docs/PHASE_SCOPE_GOVERNANCE_RULE.md`** — Permanent Phase Scope Governance Rule & Alignment Checks
4. **`CAPABILITY_MATRIX.md`** — Universal Capability Matrix & `CapabilityRule` Definitions
5. **`PERMISSION_MATRIX.md`** — Security Classifications, Special Access & SAF Storage Protocol
6. **`ACTION_CONTRACTS.md`** — Target-Aware Action Contracts & `VerificationStrategy` Protocol
7. **`OBSERVATION_MODEL.md`** — Single-Root Observation Snapshot Generator & Composite Node Identity
8. **`LOGGING_AND_AUDIT.md`** — Structured Event Logging & Unified `agent.db` WAL Storage Accounting
9. **`LOW_RAM_DESIGN.md`** — Cross-Phase Low-RAM Architecture & Engineering Memory Targets
10. **`MEMORY_AND_LEARNING.md`** — Memory Subsystem, `GoalState`, `TaskLifecycle` & `ExecutionLock`
11. **`STT_TTS_ARCHITECTURE.md`** — Multilingual Speech Engine & `LanguageEngineStatus` Capabilities
12. **`EXTERNAL_KNOWLEDGE_ARCHITECTURE.md`** — External Research, Chat Import & Prompt Injection Defenses
13. **`HARDWARE_CAPABILITIES.md`** — Hardware Control Matrix & Risk Policy Integration
14. **`TESTING_STRATEGY.md`** — 3-Level Testing Pyramid & Master Certification Framework
15. **`RESEARCH_SOURCES.md`** — Research Sources & Architectural Inferences
16. **`DEFINITION_OF_DONE.md`** — Multi-Tiered Definition of Done & Prohibitions
17. **`RISKS_AND_LIMITATIONS.md`** — Architectural Risks & Platform Boundaries
18. **`docs/PHASE_0_9_AUDIT.md`** — Phase 0.9 Audit & Readiness Report
19. **`docs/PHASE_0_9_FINAL_AUDIT.md`** — Phase 0.9 Final Master Architecture Audit & Freeze Gate Report
20. **`docs/PHASE_0_9_CONTRACT_AUDIT.md`** — Phase 0.9 Contract Audit & Freeze Gate Deliverables
21. **`docs/CROSS_DOCUMENT_CONSISTENCY_MATRIX.md`** — Master Cross-Document Consistency Matrix
22. **`docs/PHASE_1_COMPLETION_REPORT.md`** — Phase 1 Documentation & Evidence Package
23. **`docs/PHASE_3_1_COMPLETION_REPORT.md`** — Phase 3.1 Implementation & Verification Report

---

## Key Architecture Mandates

- **Primary Target Baseline:** Android 8.1 / API level 27 on low-RAM (1–2 GB RAM) hardware.
- **Universal Pipeline:** Console, Movable Overlay, Voice, Workflows, Solvers, and AI Planners converge into a single `NormalizedCommand` -> `GoalDispatcher` execution path.
- **No Fake Success:** Execution requires live node reacquisition, actionable ancestor traversal, and target-aware post-action UI observation diff verification.
- **Single Device Execution Lock:** Only ONE foreground device-control transaction manipulates the Accessibility execution channel at a time.
- **Unified Persistence:** Operational data, audit events, active tasks, and execution events are stored in SQLite database `agent.db`, whereas long-term memory is persisted via `DurableMemoryStorageManager` (`/sdcard/LocalAgent/memory/`) with SHA-256 checksums to survive uninstalls.
- **Untrusted External Data Boundary:** Web pages, browser content, chat exports, and AI responses are strictly classified as `<untrusted_external_content>` data and MUST NEVER be converted directly into executable system commands without passing `ActionPolicyEngine` checks.
