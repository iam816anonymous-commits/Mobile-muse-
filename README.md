# README.md — LocalAgent Repository Index & Architecture Overview

## Overview

**LocalAgent** is a low-RAM, offline-first Android device agent capable of observing, reasoning about, and safely executing actions on Android devices. It prioritizes **deterministic automation first**, with optional AI learning, research, and planning layers sitting above it.

This repository is currently in **Phase 0.9 Architecture Freeze**. All architecture, capability matrices, permission models, observation models, action contracts, storage budgets, testing strategies, and phase dependency contracts are fully specified and frozen in the specification package.

---

## Phase 0.9 Specification Document Index

All architectural specifications are located in the repository root directory:

1. **`ARCHITECTURE.md`** — Core Architecture, Unified Pipeline & Directory Structure
2. **`PHASE_PLAN.md`** — 23-Phase Implementation Roadmap & Phase Contracts
3. **`CAPABILITY_MATRIX.md`** — Universal Capability Matrix & `CapabilityRule` Definitions
4. **`PERMISSION_MATRIX.md`** — Security Classifications, Special Access & SAF Storage Protocol
5. **`ACTION_CONTRACTS.md`** — Target-Aware Action Contracts & `VerificationStrategy` Protocol
6. **`OBSERVATION_MODEL.md`** — Single-Root Observation Snapshot Generator & Composite Node Identity
7. **`LOGGING_AND_AUDIT.md`** — Structured Event Logging & Unified `agent.db` WAL Storage Accounting
8. **`LOW_RAM_DESIGN.md`** — Cross-Phase Low-RAM Architecture & Engineering Memory Targets
9. **`MEMORY_AND_LEARNING.md`** — Memory Subsystem, `GoalState`, `TaskLifecycle` & `ExecutionLock`
10. **`STT_TTS_ARCHITECTURE.md`** — Multilingual Speech Engine & `LanguageEngineStatus` Capabilities
11. **`EXTERNAL_KNOWLEDGE_ARCHITECTURE.md`** — External Research, Chat Import & Prompt Injection Defenses
12. **`HARDWARE_CAPABILITIES.md`** — Hardware Control Matrix & Risk Policy Integration
13. **`TESTING_STRATEGY.md`** — 3-Level Testing Pyramid & Master Certification Framework
14. **`RESEARCH_SOURCES.md`** — Research Sources & Architectural Inferences
15. **`DEFINITION_OF_DONE.md`** — Multi-Tiered Definition of Done & Prohibitions
16. **`RISKS_AND_LIMITATIONS.md`** — Architectural Risks & Platform Boundaries
17. **`docs/PHASE_0_9_AUDIT.md`** — Phase 0.9 Audit & Readiness Report
18. **`docs/PHASE_0_9_FINAL_AUDIT.md`** — Final Master Architecture Audit & Freeze Gate Report

---

## Key Architecture Mandates

- **Primary Target Baseline:** Android 8.1 / API level 27 on low-RAM (1–2 GB RAM) hardware.
- **Universal Pipeline:** Console, Movable Overlay, Voice, Workflows, Solvers, and AI Planners converge into a single `NormalizedCommand` -> `GoalDispatcher` execution path.
- **No Fake Success:** Execution requires live node reacquisition, actionable ancestor traversal, and target-aware post-action UI observation diff verification.
- **Single Device Execution Lock:** Only ONE foreground device-control transaction manipulates the Accessibility execution channel at a time.
- **Unified Persistence:** All persistent data (events, episodes, workflows, knowledge, semantic data) stored in tables inside a single `agent.db` SQLite database with WAL accounting under a 30 MB maximum cap.
- **Untrusted External Data Boundary:** Web pages, browser content, chat exports, and AI responses are strictly classified as `<untrusted_external_content>` data and MUST NEVER be converted directly into executable system commands without passing `ActionPolicyEngine` checks.
