# PHASE_PLAN.md — LocalAgent Implementation Roadmap & Phase Dependencies

## 1. Overview & Build Philosophy

The implementation of **LocalAgent** is organized into **23 distinct, strictly ordered phases** (Phase 0 through Phase 22).

### Mandatory Phase Scope Governance Rule
All phase implementations are governed by the mandatory project rule defined in [`docs/PHASE_SCOPE_GOVERNANCE_RULE.md`](docs/PHASE_SCOPE_GOVERNANCE_RULE.md).
Developers and automated agents MUST perform an explicit **Phase Alignment Check** before implementing any feature, test, or architectural change. Functionality from another phase must NEVER be silently pulled forward or merged into the current phase without explicit project-owner approval.

### Core Contract Rules for Every Phase
Every phase is governed by an explicit **Phase Contract** defining:
- **INPUT:** Prerequisites and artifacts passed from prior phases.
- **DEPENDENCIES:** Internal module and Android API dependencies.
- **DELIVERABLES:** Source code, configuration, and documentation created in this phase.
- **CAPABILITIES:** Capabilities unlocked or implemented in this phase.
- **TESTS:** Tier A (Unit), Tier B (Contract/Parity), and Tier C (Device/E2E) tests.
- **EVIDENCE:** JSON reports, log traces, and diagnostic dumps proving execution.
- **EXIT CRITERIA:** Unambiguous readiness criteria required to freeze the phase.

---

## 2. Comprehensive Phase Contracts (Phases 0 – 22)

### Phase 0 — Specification & Planning
- **INPUT:** Architecture requirements and research sources.
- **DEPENDENCIES:** None.
- **DELIVERABLES:** 16 Markdown specification files in repository root.
- **CAPABILITIES:** None (Planning only).
- **TESTS:** Specification consistency and audit verification.
- **EVIDENCE:** `docs/PHASE_0_9_AUDIT.md`.
- **EXIT CRITERIA:** Architecture frozen, 100% specification audit pass.

### Phase 1 — Foundation & Domain Core
- **INPUT:** Phase 0 specifications.
- **DEPENDENCIES:** `core` module, Kotlin Standard Library, Android API 27 SDK.
- **DELIVERABLES:** `NormalizedCommand`, `CapabilityRegistry`, `ActionPolicyEngine`, `GoalState`, `TaskLifecycle`, priority `GoalDispatcher`.
- **CAPABILITIES:** Command parsing, risk policy classification, goal state tracking, execution locking.
- **TESTS:** Tier A unit tests for command parsing, policy evaluation, queue priority, and lifecycle state transitions.
- **EVIDENCE:** JUnit test execution report.
- **EXIT CRITERIA:** 100% unit test pass, zero external dependencies.

### Phase 2 — Persistent Storage & Unified Logging
- **INPUT:** Phase 1 domain core models.
- **DEPENDENCIES:** `storage` module, Android SQLite / Room API 27, Storage Access Framework.
- **DELIVERABLES:** Unified `agent.db` schema (`agent_events`, `episodes`, `workflows`, `semantic_data`, `knowledge`), `EventLogger`, `RoomEventRepository`, `DurableMemoryStorageManager` (`DurableRecord`, `DurableMemoryStorageProvider`), `LogRetentionManager` with WAL checkpointing.
- **CAPABILITIES:** Persistent structured logging, correlation ID tracing, WAL file accounting, durable long-term memory storage.
- **TESTS:** Tier A DB insertion and durable memory unit tests, Tier B SQLite migration, WAL checkpoint, and uninstall survival tests on API 27.
- **EVIDENCE:** DB row insertion logs, WAL file size report (< 5 MB), durable storage status diagnostics.
- **EXIT CRITERIA:** 1,000 continuous event insertions without dropping logs, app-private footprint <= 30 MB, durable memory storage abstraction active.

### Phase 3 — Permission & Capability Manager
- **INPUT:** Phase 1 `CapabilityRegistry`, Phase 2 `EventLogger`.
- **DEPENDENCIES:** `system` module, Android `PackageManager`, `Settings` APIs.
- **DELIVERABLES:** `PermissionManager`, API compatibility adapters (API 27–36+), in-app Permission Center UI.
- **CAPABILITIES:** Runtime permission checking, special access detection, settings intent launching, passive degradation monitoring.
- **TESTS:** Tier A permission status unit tests, Tier B settings intent generation and passive degradation tests.
- **EVIDENCE:** Permission Center UI screenshots/layout dumps, intent verification logs.
- **EXIT CRITERIA:** All special access setting intents verified on API 27 baseline; passive degradation confirmed when Accessibility is unbound.

### Phase 4 — Accessibility Service Foundation
- **INPUT:** Phase 3 permission manager.
- **DEPENDENCIES:** `accessibility` module, Android `AccessibilityService` API 27.
- **DELIVERABLES:** `AgentAccessibilityService`, passive connection lifecycle binder, service connection monitor.
- **CAPABILITIES:** Accessibility service binding, system broadcast receiver, passive event monitoring.
- **TESTS:** Tier B instrumentation tests for service connection and disconnection lifecycle events.
- **EVIDENCE:** Accessibility service binding log trace.
- **EXIT CRITERIA:** Service binds cleanly on API 27; unbinding triggers passive degradation without crashing.

### Phase 5 — Observation & Snapshot Engine
- **INPUT:** Phase 4 `AgentAccessibilityService` binding.
- **DEPENDENCIES:** `accessibility` module, `ObservationSnapshotGenerator`, API 27 `Rect` bounds APIs.
- **DELIVERABLES:** Single-root snapshot extractor, node identity generator (`NodeIdentityConfidence`), immediate `.recycle()` pipeline, `SnapshotDiffEngine`.
- **CAPABILITIES:** `UI_OBSERVE`, `SNAPSHOT_DIFF`.
- **TESTS:** Tier A diff engine unit tests, Tier B mock snapshot generation and node recycling tests.
- **EVIDENCE:** Snapshot JSON primitive dump, node recycling memory trace.
- **EXIT CRITERIA:** Single `rootInActiveWindow` call per snapshot cycle; zero un-recycled nodes; node identity confidence rated for every primitive; heap footprint < 35 MB on API 27.

### Phase 6 — Universal Action Engine
- **INPUT:** Phase 5 observation snapshots.
- **DEPENDENCIES:** `accessibility` module, `TargetResolver`, `ActionableAncestorResolver`.
- **DELIVERABLES:** Target resolution strategies, clickable/long-clickable/scrollable/editable ancestor traversal algorithms, live target re-acquisition.
- **CAPABILITIES:** Live node resolution, ancestor traversal.
- **TESTS:** Tier A target resolver unit tests (non-clickable child to clickable parent mapping), Tier B live node re-acquisition tests.
- **EVIDENCE:** Ancestor resolution diagnostic logs.
- **EXIT CRITERIA:** Child TextView "7" correctly resolves to parent MaterialButton container with `HIGH` or `EXACT` confidence.

### Phase 7 — Global & UI Action Execution
- **INPUT:** Phase 6 universal action engine.
- **DEPENDENCIES:** `accessibility` module, `VerificationStrategy` implementations.
- **DELIVERABLES:** Action contracts for `GLOBAL_BACK`, `GLOBAL_HOME`, `GLOBAL_RECENTS`, `UI_CLICK`, `UI_LONG_CLICK`, `UI_TEXT_INPUT`, `UI_SCROLL_FORWARD`, `UI_SCROLL_BACKWARD`.
- **CAPABILITIES:** Core UI automation and global navigation.
- **TESTS:** Tier A verification strategy unit tests, Tier C real-device action execution tests on Calculator and Settings apps.
- **EVIDENCE:** Pre/post action snapshot diff JSON, execution result logs.
- **EXIT CRITERIA:** `UI_CLICK`, `UI_TEXT_INPUT`, and `UI_SCROLL` verify target-aware state diffs; `GLOBAL_BACK` verifies navigation-aware diffs on API 27 physical hardware.

### Phase 8 — Application Control Engine
- **INPUT:** Phase 7 global actions.
- **DEPENDENCIES:** `system` module, `PackageManager`, `<queries>` manifest configuration.
- **DELIVERABLES:** `AppResolver`, `AppLauncher`, `LaunchVerifier`.
- **CAPABILITIES:** `APP_LAUNCH`, `APP_RESOLVE`.
- **TESTS:** Tier A package resolution unit tests, Tier C app launch and foreground verification tests.
- **EVIDENCE:** App launch verification trace.
- **EXIT CRITERIA:** Launching Settings and Calculator verifies foreground package within 3-second timeout.

### Phase 9 — Command Console UI
- **INPUT:** Phase 1 `GoalDispatcher`, Phase 7 actions, Phase 8 app launch.
- **DEPENDENCIES:** `app` module, Android View system.
- **DELIVERABLES:** Thin-client Console Activity, command prompt text input, real-time log feed viewer connected to `agent.db`.
- **CAPABILITIES:** Manual text command entry, real-time log monitoring.
- **TESTS:** Tier B Console input dispatch and log view binding tests.
- **EVIDENCE:** Console execution logs.
- **EXIT CRITERIA:** Text commands typed in Console dispatch to universal `GoalDispatcher` and display execution events in real time.

### Phase 10 — Automated Test Center
- **INPUT:** Phase 1–9 domain models and actions.
- **DEPENDENCIES:** `testing` module, `TestCenter` UI.
- **DELIVERABLES:** In-app `TestCenter` Activity, automated contract and cross-channel parity test runner.
- **CAPABILITIES:** Diagnostic self-testing, contract verification.
- **TESTS:** Tier B contract and parity test suite executed inside app.
- **EVIDENCE:** In-app test report JSON output (`evidence/contract-suite-report.json`).
- **EXIT CRITERIA:** Contract suite executes inside app with 100% pass rate.

### Phase 11 — Movable Overlay Surface
- **INPUT:** Phase 1 `GoalDispatcher`, Phase 3 overlay permission, Phase 10 `TestCenter`.
- **DEPENDENCIES:** `app` module, `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **DELIVERABLES:** Movable Overlay Service, floating icon, touch drag listener, overlay action panel (`BACK`, `HOME`, `RECENTS`, `CLICK`, `SCROLL`, `STATUS`, `HIDE`).
- **CAPABILITIES:** `OVERLAY_SHOW`, `OVERLAY_MOVE`.
- **TESTS:** Tier B overlay rendering tests, Tier B cross-channel parity tests (Overlay vs Console).
- **EVIDENCE:** Cross-channel parity test report proving Overlay and Console execute identical `NormalizedCommand`s.
- **EXIT CRITERIA:** Overlay actions produce identical execution traces and state diffs as Console commands.

### Phase 12 — Hardware & System Controls
- **INPUT:** Phase 1 `ActionPolicyEngine`, Phase 3 permission manager, Phase 7 actions.
- **DEPENDENCIES:** `system` module, `AudioManager`, `CameraManager`, `Settings.System`.
- **DELIVERABLES:** Volume, Brightness, Flashlight, Media, and Wi-Fi panel hardware controllers.
- **CAPABILITIES:** `HARDWARE_VOLUME`, `HARDWARE_BRIGHTNESS`, `HARDWARE_FLASHLIGHT`, `HARDWARE_MEDIA`, `HARDWARE_WIFI_TOGGLE`.
- **TESTS:** Tier A risk policy unit tests, Tier C hardware modification tests on physical API 27 device.
- **EVIDENCE:** Hardware state change logs.
- **EXIT CRITERIA:** Hardware actions verify system setting changes; direct vs UI automation fallbacks function correctly across API 27–36+.

### Phase 13 — Speech STT / TTS Subsystem
- **INPUT:** Phase 1 `GoalDispatcher`, Phase 9 Console.
- **DEPENDENCIES:** `voice` module, Android `SpeechRecognizer`, `TextToSpeech`.
- **DELIVERABLES:** `SpeechInputProvider`, `SpeechOutputProvider`, multilingual normalizer (EN, TE, KN, HI), `LanguageEngineStatus` capability detector.
- **CAPABILITIES:** `SPEECH_STT`, `SPEECH_TTS`.
- **TESTS:** Tier A multilingual normalization unit tests, Tier C speech recognition and synthesis tests.
- **EVIDENCE:** Spoken transcript to `NormalizedCommand` conversion logs.
- **EXIT CRITERIA:** Utterances in EN, TE, KN, HI convert accurately to normalized commands; speech engines destroyed immediately when idle.

### Phase 14 — Workflow & Automation Engine
- **INPUT:** Phase 7 UI actions, Phase 8 app control.
- **DEPENDENCIES:** `core` module, `WorkflowEngine`.
- **DELIVERABLES:** Deterministic workflow execution engine, sequential step dispatcher, retry policies, step verification.
- **CAPABILITIES:** `WORKFLOW_EXECUTE`.
- **TESTS:** Tier A workflow step sequence unit tests, Tier C multi-step workflow execution tests.
- **EVIDENCE:** Multi-step workflow execution trace.
- **EXIT CRITERIA:** Multi-step automation flows execute deterministically with explicit per-step verification.

### Phase 15 — Episodic Memory & Learning
- **INPUT:** Phase 2 `agent.db`, Phase 14 workflow engine.
- **DEPENDENCIES:** `memory` module, Room `episodes` and `workflows` tables, `DurableMemoryStorageProvider`.
- **DELIVERABLES:** Episodic task log store, procedural workflow extractor, live target re-validation protocol.
- **CAPABILITIES:** Procedural learning, workflow re-validation.
- **TESTS:** Tier A learning extraction unit tests, Tier B workflow storage and stale target rejection tests.
- **EVIDENCE:** Learned workflow DB records, target re-validation logs.
- **EXIT CRITERIA:** Replaying learned workflows re-validates live UI nodes; stale targets trigger re-resolution rather than blind replay.

### Phase 16 — Browser Research Foundation
- **INPUT:** Phase 7 UI actions, Phase 8 app control.
- **DEPENDENCIES:** `research` module, Chrome / Web Browser Accessibility observation.
- **DELIVERABLES:** Web page observation engine, element interaction, `<untrusted_external_content>` data wrapper.
- **CAPABILITIES:** `BROWSER_NAVIGATE`, `BROWSER_OBSERVE`.
- **TESTS:** Tier A untrusted text wrapping unit tests, Tier C browser page navigation tests.
- **EVIDENCE:** Web page snapshot primitive dump with untrusted data tags.
- **EXIT CRITERIA:** Web page content extracted safely and tagged as untrusted data; execution pipeline isolated from prompt injection.

### Phase 17 — External Knowledge Ingestion
- **INPUT:** Phase 16 browser research foundation, Phase 15 memory.
- **DEPENDENCIES:** `research` module, Storage Access Framework (SAF).
- **DELIVERABLES:** SAF document picker integration, `ChatExportParser` (ChatGPT/Gemini exports), provenance hash generator, `knowledge` table store.
- **CAPABILITIES:** `EXTERNAL_KNOWLEDGE_IMPORT`.
- **TESTS:** Tier A SAF URI reading and parser unit tests.
- **EVIDENCE:** Parsed knowledge record with SHA-256 provenance hash.
- **EXIT CRITERIA:** Export files imported via SAF without broad storage permissions; provenance hash logged in `agent.db`.

### Phase 18 — Structured Problem Solver
- **INPUT:** Phase 5 observation snapshot, Phase 7 UI actions.
- **DEPENDENCIES:** `solver` module, pure Kotlin backtracking / CSP solver.
- **DELIVERABLES:** Grid/board UI state extractor, deterministic Sudoku / form solver, action step generator.
- **CAPABILITIES:** `STRUCTURED_PROBLEM_SOLVER`.
- **TESTS:** Tier A solver algorithm unit tests (100% solve accuracy on benchmark Sudoku puzzles), Tier C live UI board solving test.
- **EVIDENCE:** Solved puzzle board state verification diff.
- **EXIT CRITERIA:** Visual grid extracted from screen, solved deterministically in memory, and numbers populated into UI fields with verified diffs.

### Phase 19 — Trip & General Research Engine
- **INPUT:** Phase 16 browser research, Phase 17 external knowledge.
- **DEPENDENCIES:** `research` module, `ResearchPlanner`.
- **DELIVERABLES:** Multi-step research goal decomposer, travel option fact extractor, research report generator with citations.
- **CAPABILITIES:** `TRIP_RESEARCH_ENGINE`.
- **TESTS:** Tier A goal decomposition unit tests, Tier C multi-step travel research execution test.
- **EVIDENCE:** Generated trip itinerary report JSON with source citations.
- **EXIT CRITERIA:** Research goal executed across search queries, facts extracted into `agent.db`, and structured itinerary presented with citations.

### Phase 20 — On-Device / Cloud AI Planner
- **INPUT:** Phase 1 `NormalizedCommand`, Phase 1 `ActionPolicyEngine`, Phase 19 research engine.
- **DEPENDENCIES:** `ai` module, Pluggable LLM Provider API adapters.
- **DELIVERABLES:** Pluggable AI planner adapter, structured JSON plan output validator, safety policy interceptor.
- **CAPABILITIES:** `AI_PLANNING`.
- **TESTS:** Tier A JSON plan schema validation and prompt injection interception unit tests.
- **EVIDENCE:** AI plan output JSON and policy interceptor logs.
- **EXIT CRITERIA:** AI planner outputs valid `NormalizedCommand` JSON plans; high-risk commands blocked by `ActionPolicyEngine` until user confirms.

### Phase 21 — Resource Hardening & Recovery
- **INPUT:** Phase 1–20 modules.
- **DEPENDENCIES:** `core` module, `ResourceManager`, Android Low Memory Killer (LMK) recovery semantics.
- **DELIVERABLES:** Process death recovery protocol, `agent.db` LMK state restore, cache eviction manager, resource threshold monitor.
- **CAPABILITIES:** Process recovery, resource hardening.
- **TESTS:** Tier D low-RAM stress tests, simulated LMK process termination and state resume tests on API 27 baseline.
- **EVIDENCE:** LMK kill and resume trace log, continuous 1,000-action dispatch heap footprint report.
- **EXIT CRITERIA:** Zero state loss after simulated LMK process kill; active workflows resume gracefully; heap footprint remains within engineering target thresholds.

### Phase 22 — Master Device Certification
- **INPUT:** All Phase 0–21 deliverables and test suites.
- **DEPENDENCIES:** Physical Android 8.1 / API level 27 reference device, master automated test suite.
- **DELIVERABLES:** `docs/MASTER_CERTIFICATION_REPORT.md`, diagnostic log archive export.
- **CAPABILITIES:** Complete Agent System Certification.
- **TESTS:** Full Tier A, Tier B, Tier C, and Tier D automated test suite execution on physical API 27 hardware.
- **EVIDENCE:** Certification execution report (`evidence/master-certification-report.json`).
- **EXIT CRITERIA:** 100% pass rate across all test contracts on physical API 27 hardware; zero unresolved memory leaks or state corruption issues.

---

## 3. Phase Dependency Graph

```text
Phase 0 (Specs) ──► Phase 0.9 (Audit)
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
