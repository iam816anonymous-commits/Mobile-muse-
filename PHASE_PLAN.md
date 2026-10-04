# PHASE_PLAN.md — LocalAgent Implementation Roadmap & Phase Dependencies

## 1. Overview & Build Philosophy

The implementation of **LocalAgent** is organized into **21 distinct, strictly ordered phases** (Phase 0 through Phase 20).

### Core Rules of Progression
1. **Incremental Proof:** Each phase must be fully built, unit tested, instrumentation tested, and verified on real device hardware before progressing to the subsequent phase.
2. **No Skipping Steps:** Higher-level capabilities (e.g., Overlay, Voice, AI Reasoning) must never be built before their foundational execution, logging, and permission contracts are proven.
3. **Definition of Done Enforced:** A phase is complete **only** when all unit tests pass AND real-device test execution criteria specified in `DEFINITION_OF_DONE.md` are met.

---

## 2. Master Implementation Phase Table

| Phase | Phase Name | Primary Goal | Hard Dependencies | Deliverable / Verification |
|---|---|---|---|---|
| **Phase 0** | Specification & Planning | Freeze core specs, contracts, matrices, and test strategies. | None | 16 Spec MD files in Repo Root |
| **Phase 1** | Foundation & Domain Core | Build data models, `NormalizedCommand`, interfaces, lifecycle state machine. | Phase 0 | Domain Core Module & Unit Tests |
| **Phase 2** | Persistent Storage & Logging | Build SQLite/Room DB, structured `EventLogger`, log rotation & storage policies. | Phase 1 | Event Persistence & Audit Unit Tests |
| **Phase 3** | Permission & Capability Manager | Build runtime & special permission manager, API compatibility adapters (API 27–36+). | Phase 1, 2 | Permission Center UI & Diagnostics |
| **Phase 4** | Accessibility Service Foundation | Implement `AgentAccessibilityService`, service lifecycle, active window binding. | Phase 2, 3 | A11y Service Binding & Event Tests |
| **Phase 5** | Observation Engine | Implement UI tree scanning, primitive extraction, snapshot diffing, node recycling. | Phase 4 | UI Snapshot Generator & Diff Tests |
| **Phase 6** | Global Action Execution | Execute BACK, HOME, RECENTS, Notifications, Quick Settings via Accessibility. | Phase 4, 5 | Global Actions Execution & Verification |
| **Phase 7** | Target Resolution & UI Actions | Implement ancestor traversal (Clickable, Scrollable, Editable) and CLICK/SCROLL/TEXT. | Phase 5, 6 | UI Action Ancestor & Diff Verification |
| **Phase 8** | Application Control Engine | Build `AppResolver`, `AppLauncher`, activity resolution, foreground verification. | Phase 6, 7 | App Launch & Foreground Verification |
| **Phase 9** | Command Console UI | Thin-client Console UI submitting commands to universal `GoalDispatcher`. | Phase 1, 7, 8 | Console Execution & Real-Time Logs |
| **Phase 10** | Movable Overlay Surface | Implement `TYPE_APPLICATION_OVERLAY` floating control routing to `GoalDispatcher`. | Phase 3, 9 | Movable Overlay & Action Parity |
| **Phase 11** | Hardware & System Controls | Implement Volume, Brightness, Flashlight, Media Controls with API tier checks. | Phase 3, 7 | Hardware Control Matrix Verification |
| **Phase 12** | Speech STT / TTS Subsystem | Implement API 27 STT/TTS providers, multilingual support (EN, TE, KN, HI). | Phase 1, 9 | Speech Command Input & TTS Feedback |
| **Phase 13** | Workflow & Automation Engine | Implement triggers, conditions, sequential actions, retry policies, and timeouts. | Phase 7, 8, 9 | Deterministic Workflow Execution |
| **Phase 14** | Episodic Memory & Learning | Build persistent execution history, procedural workflow learning & revalidation. | Phase 2, 13 | Workflow Learning & Storage Tests |
| **Phase 15** | Browser Agent & UI Interaction | Implement browser page navigation, web element observation, injection safeguards. | Phase 7, 8, 14 | Web Research & Observation Tests |
| **Phase 16** | External Knowledge Integration | Knowledge import pipeline for ChatGPT/Gemini exports, web research sanitization. | Phase 14, 15 | Untrusted Knowledge Importer Tests |
| **Phase 17** | On-Device / Cloud AI Planner | Optional LLM intent classification & plan generation outputting `NormalizedCommand`s. | Phase 1, 13, 16 | AI Intent to Execution Pipeline |
| **Phase 18** | Autonomous Device Agent | Closed-loop Goal → Observe → Plan → Execute → Verify → Re-plan → Learn cycle. | Phase 13–17 | Autonomous Task Resolution |
| **Phase 19** | Low-RAM & Resource Optimization | Enforce RAM/power budgets, process recovery after LMK death, cache eviction. | Phase 1–18 | Low-RAM Survival & Stress Tests |
| **Phase 20** | Full Device Certification | Execute master automated test suite on physical Android 8.1 (API 27) device. | Phase 1–19 | Master Certification Audit Report |

---

## 3. Detailed Phase Descriptions & Key Tasks

### Phase 0 — Specification & Planning
- **Tasks:** Conduct deep research on MacroDroid, Automate, Tasker, Android API docs (API 27–36+). Generate the 16 specification documents.
- **Output:** Fully frozen spec suite in repository root.

### Phase 1 — Foundation & Domain Core
- **Tasks:** Setup multi-module Kotlin project (`core`, `app`, `accessibility`, `system`, `storage`, `voice`, `memory`, `testing`). Build `NormalizedCommand`, `ActionType`, `CommandRegistry`, `GoalDispatcher`, and lifecycle state machine (`ACTIVE`, `IDLE`, `PAUSED`).
- **Tests:** 100% unit test coverage for command parsing, normalization, and dispatcher queueing.

### Phase 2 — Persistent Storage & Universal Logging
- **Tasks:** Implement Room/SQLite database for structured event logging (`logs/agent-events.db`). Create `EventLogger` singleton supporting correlation IDs, timestamps, and log rotation (max 20 MB).
- **Tests:** Unit tests verifying DB insertion, querying, log rotation, and crash recovery.

### Phase 3 — Permission & Capability Manager
- **Tasks:** Build `PermissionManager` detecting runtime permissions (`RECORD_AUDIO`, `POST_NOTIFICATIONS`) and special access (`Accessibility`, `Overlay`, `UsageAccess`, `WriteSettings`, `NotificationListener`). Implement API compatibility adapters for API levels 27 through 36+. Build in-app Permission Center UI.
- **Tests:** Unit tests for permission state checking and mock setting launches.

### Phase 4 — Accessibility Service Foundation
- **Tasks:** Implement `AgentAccessibilityService` extending `AccessibilityService`. Configure accessibility XML service info (flags, event types). Build service lifecycle binding and connection status broadcast.
- **Tests:** Instrumentation tests confirming service startup, event receiving, and disconnection handling.

### Phase 5 — Observation Engine
- **Tasks:** Build `ObservationSnapshotGenerator`. Iterate active window nodes, extract primitive data (`text`, `bounds`, `clickable`, `editable`, `scrollable`, `viewId`), immediately recycle live `AccessibilityNodeInfo` instances. Build `SnapshotDiffEngine`.
- **Tests:** Unit and instrumentation tests comparing pre/post action snapshots.

### Phase 6 — Global Action Execution
- **Tasks:** Implement global actions (`BACK`, `HOME`, `RECENTS`, `NOTIFICATIONS`, `QUICK_SETTINGS`) using `AccessibilityService.performGlobalAction()`. Implement post-action foreground package/window verification.
- **Tests:** Real-device tests confirming system navigation and foreground state verification.

### Phase 7 — Target Resolution & UI Actions
- **Tasks:** Build `TargetResolver` with actionable ancestor traversal (`findClickableAncestor`, `findLongClickableAncestor`, `findScrollableAncestor`, `findEditableTarget`). Implement `CLICK`, `LONG_CLICK`, `TEXT_INPUT`, `SCROLL_FORWARD`, `SCROLL_BACKWARD`.
- **Tests:** Tests verifying child-to-ancestor click propagation (e.g., Calculator digit button), input verification, and scroll diffing.

### Phase 8 — Application Control Engine
- **Tasks:** Build `AppResolver` (querying `PackageManager`), `AppLauncher`, and `LaunchVerifier`. Resolve package names from app labels. Verify foreground package/activity post-launch within timeout.
- **Tests:** App launch tests across standard system applications (Settings, Calculator, Files).

### Phase 9 — Command Console UI
- **Tasks:** Build thin-client Console Activity. User types text commands (`click 7`, `launch Settings`, `back`). Commands are parsed into `NormalizedCommand` and dispatched via `GoalDispatcher`. Display real-time persistent log feeds.
- **Tests:** Cross-channel action parity unit and integration tests.

### Phase 10 — Movable Overlay Surface
- **Tasks:** Implement floating overlay view using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`. Add drag gesture handling. Wire overlay action buttons (`BACK`, `HOME`, `RECENTS`, `SCROLL`, `CLICK`) to send `NormalizedCommand`s to `GoalDispatcher`.
- **Tests:** Parity tests ensuring overlay actions follow identical execution paths as Console.

### Phase 11 — Hardware & System Controls
- **Tasks:** Implement hardware capability controllers (`VolumeController`, `BrightnessController`, `FlashlightController`, `MediaController`). Enforce permission and API capability checks (`WRITE_SETTINGS`, `MODIFY_AUDIO_SETTINGS`).
- **Tests:** Hardware API modification tests and capability availability verification.

### Phase 12 — Speech STT / TTS Subsystem
- **Tasks:** Implement `SpeechInputProvider` wrapping Android `SpeechRecognizer` (API 27+). Implement `SpeechOutputProvider` wrapping Android `TextToSpeech`. Add language detection for English, Telugu, Kannada, Hindi. Enforce resource release when idle.
- **Tests:** STT intent parsing tests and TTS engine initialization/synthesis tests.

### Phase 13 — Workflow & Automation Engine
- **Tasks:** Implement deterministic trigger-condition-action workflow engine. Support sequential steps, delays, retries, and explicit verification checks at each step.
- **Tests:** Multi-step automation flow execution and failure recovery tests.

### Phase 14 — Episodic Memory & Learning
- **Tasks:** Implement episodic task log storage and procedural workflow learning. Store successful action sequences for specific package/activity contexts. Enforce target re-validation before replaying learned workflows.
- **Tests:** Workflow storage, retrieval, revalidation, and stale target rejection tests.

### Phase 15 — Browser Agent & Web Interaction
- **Tasks:** Implement web page observation and element interaction for browser applications. Enforce security sandbox ensuring web page text is treated as untrusted data (prompt injection defense).
- **Tests:** Web interaction and untrusted data handling tests.

### Phase 16 — External Knowledge Integration
- **Tasks:** Build knowledge import pipeline supporting ChatGPT/Gemini exported chats, text files, and web summaries. Extract structured knowledge with source metadata and provenance.
- **Tests:** Knowledge parser, provenance tracking, and sanitization tests.

### Phase 17 — On-Device / Cloud AI Planner
- **Tasks:** Pluggable AI planning layer (local lightweight model or cloud fallback). AI receives structured observation snapshots and outputs strict JSON plans conforming to `NormalizedCommand` schemas.
- **Tests:** Planner output validation against safety policies and schema rules.

### Phase 18 — Autonomous Device Agent
- **Tasks:** Assemble full closed-loop agent loop: Goal Input → Observe UI → Plan Actions → Execute via Universal Engine → Verify Diffs → Re-plan on Failure → Update Memory.
- **Tests:** End-to-end autonomous task resolution tests.

### Phase 19 — Low-RAM & Resource Optimization
- **Tasks:** Build `ResourceManager` monitoring RAM, battery, and storage. Enforce memory trimming, snapshot size caps, cache eviction, and LMK process recovery.
- **Tests:** Low-memory simulation tests on Android 8.1 API 27 baseline.

### Phase 20 — Full Device Certification
- **Tasks:** Run complete automated master test suite on physical API 27 hardware. Generate diagnostic report certifying all capabilities, cross-channel parity, and resource metrics.
- **Tests:** Full certification execution.

---

## 4. Phase Dependency Graph

```text
Phase 0 (Specs)
  └── Phase 1 (Core Domain)
        ├── Phase 2 (Storage & Logging)
        │     └── Phase 4 (Accessibility Service)
        │           ├── Phase 5 (Observation Engine)
        │           │     ├── Phase 6 (Global Actions)
        │           │     │     └── Phase 7 (UI Actions)
        │           │     │           ├── Phase 8 (App Control)
        │           │     │           │     └── Phase 9 (Console UI)
        │           │     │           │           └── Phase 10 (Movable Overlay)
        │           │     │           └── Phase 11 (Hardware Control)
        │           │     │           └── Phase 13 (Workflow Engine)
        │           │     │                 ├── Phase 14 (Episodic Memory)
        │           │     │                 │     ├── Phase 15 (Browser Agent)
        │           │     │                 │     │     └── Phase 16 (External Knowledge)
        │           │     │                 │     └───────────┬──────────────────┘
        │           │     │                 │                 ▼
        │           │     │                 │           Phase 17 (AI Planner)
        │           │     │                 │                 │
        │           │     │                 └─────────────────┼────────────────┐
        │           │     │                                   ▼                ▼
        │           │     │                             Phase 18 (Autonomous Agent)
        │           │     │                                   │
        │           │     │                                   ▼
        │           │     │                             Phase 19 (Low-RAM Optimization)
        │           │     │                                   │
        │           │     │                                   ▼
        │           │     │                             Phase 20 (Full Certification)
        │           └─────┴───────────────────────────────────┘
        └── Phase 3 (Permission Manager) ──► (Informs Phase 4, 10, 11)
        └── Phase 12 (Speech STT/TTS)   ──► (Informs Phase 9, 18)
```
