# DEFINITION_OF_DONE.md — Definition of Done Specification

## 1. Overview & Quality Mandate

In **LocalAgent**, a phase or feature is **NEVER** considered complete merely because code compiles or unit tests pass. Because this system interacts directly with the Android platform, UI hierarchy, permissions, and low-RAM hardware, every phase must meet a rigorous, multi-tiered Definition of Done (DoD).

---

## 2. Universal Phase Completion Checklist

Before any implementation phase can be marked complete, all of the following criteria must be satisfied:

### A. Architectural & Contract Compliance
- [ ] Code strictly follows the universal execution pipeline (`NormalizedCommand` → `GoalDispatcher` → `ActionPolicyEngine` → `CapabilityRegistry` → `TargetResolver` → `ActionExecutor` → `VerificationStrategy` → `EventLogger`).
- [ ] No isolated or duplicated action execution logic exists (Overlay, Console, Voice, Automation, and AI use the exact same execution pipeline).
- [ ] No hardcoded dependencies on modern Android APIs without an API 27 (Android 8.1) fallback or compatibility adapter.
- [ ] Code is modular and dependency-light (no Jetpack Compose, no Hilt/Koin, no heavy ML frameworks).

### B. Multi-Tiered Automated Testing Verification
- [ ] **Tier A (Pure Unit Tests):** 100% of pure Kotlin domain logic unit tests pass via `./gradlew test`.
- [ ] **Tier B (Contract & Parity Tests):** In-app `TestCenter` verifies action contract execution and cross-channel parity (Console vs Overlay vs Voice) without requiring a physical device.
- [ ] **Tier C (Instrumentation Tests):** Android instrumentation tests pass via `./gradlew connectedAndroidTest`.
- [ ] **Negative & Policy Tests:** Tests explicitly verify graceful handling of missing permissions, missing targets, disabled accessibility service, stale nodes, action risk policy blocks, and process death.

### C. Real-Device Physical Verification (Android 8.1 / API 27 Baseline)
- [ ] Action verified on a physical device or emulator running Android 8.1 (API 27).
- [ ] Action execution proven via action-specific `VerificationStrategy` state diffs (e.g., text changed, window navigated, checked state toggled), not just `performAction() == true`.
- [ ] No UI freezing, ANR (Application Not Responding), or memory leaks during continuous action dispatch.

### D. Logging & Audit Verification
- [ ] Every action dispatch, target resolution step, API result, observation diff, and error produces a structured entry in `agent.db`.
- [ ] Log entries include timestamp, correlation ID, subsystem, action type, package/activity, result code, and execution duration.
- [ ] Unified SQLite storage limits and WAL/SHM file size policies are enforced.

### E. Resource & Cross-Phase Low-RAM Verification
- [ ] Heap footprint remains within low-RAM targets (< 35 MB during active execution, < 15 MB idle).
- [ ] All acquired `AccessibilityNodeInfo` instances are recycled immediately after snapshot generation via explicit `.recycle()` calls.
- [ ] Idle background state uses ~0% CPU and zero active polling timers.

---

## 3. Phase-Specific Completion Gates

### Phase 1: Foundation & Domain Core
- [ ] `NormalizedCommand` schema finalized and serializable.
- [ ] `CapabilityRegistry` and `ActionPolicyEngine` initialized with risk tiers (LOW, MEDIUM, HIGH, CRITICAL).
- [ ] Thread-safe `GoalDispatcher` unit tests pass with concurrent command dispatch.

### Phase 2: Persistent Storage & Logging
- [ ] Unified SQLite `agent.db` creates tables correctly on API 27.
- [ ] `EventLogger` writes 1,000 continuous structured logs without dropping events or exceeding storage quotas (accounting for DB, WAL, and SHM files).
- [ ] Automated log retention purges expired records when database reaches size limit.

### Phase 3: Permission & Capability Manager
- [ ] In-app Permission Center correctly reflects status for runtime permissions and special access (Accessibility, Overlay, Usage Access, Write Settings, Notification Listener).
- [ ] Launching system permission settings screens works seamlessly on API 27 through API 36+.

### Phase 4: Accessibility Service Foundation
- [ ] `AgentAccessibilityService` binds successfully on Android 8.1.
- [ ] Passive service lifecycle handles disconnections gracefully; actions return `ACCESSIBILITY_UNAVAILABLE` while non-accessibility features (Console, Settings, Overlay, Logging) remain fully operational.

### Phase 5: Observation Engine
- [ ] Single `rootInActiveWindow` snapshot extraction completes in < 100ms.
- [ ] Node identity matching strategy successfully tracks nodes across screen updates using resource ID, bounds, text, and structural path.
- [ ] All `AccessibilityNodeInfo` objects are recycled without native C++ memory leaks.

### Phase 6: Universal Action Engine
- [ ] Actionable ancestor traversal (`findClickableAncestor`, `findLongClickableAncestor`, `findScrollableAncestor`, `findEditableTarget`) correctly resolves target views.
- [ ] Live node re-acquisition prevents stale node exceptions.

### Phase 7: Global & UI Action Execution
- [ ] `BACK`, `HOME`, `RECENTS`, `CLICK`, `LONG_CLICK`, `TEXT_INPUT`, and `SCROLL` execute and verify action-specific UI state diffs.
- [ ] `BACK` action uses navigation-aware verification strategy rather than relying solely on package/activity changes.

### Phase 8: Application Control Engine
- [ ] `AppResolver` maps app labels to installed package names on API 27+.
- [ ] `AppLauncher` launches target app and verifies foreground package within 3-second timeout.

### Phase 9: Command Console UI
- [ ] Text commands typed in Console execute via `GoalDispatcher`.
- [ ] Console log view displays real-time execution events from `EventLogger`.

### Phase 10: Automated Test Center
- [ ] In-app `TestCenter` UI executes contract and parity test suites without physical device interaction.

### Phase 11: Movable Overlay Surface
- [ ] Overlay renders using `TYPE_APPLICATION_OVERLAY` on API 27+.
- [ ] Overlay buttons dispatch identical `NormalizedCommand`s as Console and achieve identical execution results.

### Phase 12: Hardware & System Controls
- [ ] Volume, Brightness, and Wi-Fi panel controls adjust system settings when permissions are granted.
- [ ] Unsupported or restricted hardware actions return explicit risk/permission error codes.

### Phase 13: Speech STT / TTS Subsystem
- [ ] Speech recognizer captures multilingual commands (EN, TE, KN, HI) and destroys recognizer immediately when idle.

### Phase 14: Workflow & Automation Engine
- [ ] Sequential multi-step workflows execute deterministically.

### Phase 15: Episodic Memory & Learning
- [ ] Successful action sequences stored in procedural memory and re-validated against live UI before replay.

### Phase 16: Browser Research Foundation
- [ ] Browser elements observed safely; webpage content isolated from command pipeline.

### Phase 17: External Knowledge Ingestion
- [ ] Exported ChatGPT/Gemini chat files parsed into structured knowledge entries with source provenance hashes.

### Phase 18: Structured Problem Solver
- [ ] Grid/board state extracted into structured model and solved via deterministic solver (e.g. Sudoku).

### Phase 19: Trip & General Research Engine
- [ ] Multi-step research goal decomposed, travel options extracted, and itinerary generated with citations.

### Phase 20: On-Device / Cloud AI Planner
- [ ] AI planner outputs strict JSON plans conforming to `NormalizedCommand` schema and passing `ActionPolicyEngine` checks.

### Phase 21: Resource Hardening & Recovery
- [ ] Agent state resumes seamlessly after simulated Low Memory Killer (LMK) process termination.

### Phase 22: Master Device Certification
- [ ] Master automated test runner executes on physical API 27 hardware with 100% test pass rate.
