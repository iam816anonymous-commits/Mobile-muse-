# DEFINITION_OF_DONE.md — Definition of Done Specification

## 1. Overview & Quality Mandate

In **LocalAgent**, a phase or feature is **NEVER** considered complete merely because code compiles or unit tests pass. Because this system interacts directly with the Android platform, UI hierarchy, permissions, and low-RAM hardware, every phase must meet a rigorous, multi-tiered Definition of Done (DoD).

---

## 2. Universal Phase Completion Checklist

Before any implementation phase can be marked complete, all of the following criteria must be satisfied:

### A. Architectural & Contract Compliance
- [ ] Code strictly follows the universal execution pipeline (`NormalizedCommand` → `GoalDispatcher` → `CapabilityResolver` → `TargetResolver` → `ActionExecutor` → `ObservationVerifier` → `EventLogger`).
- [ ] No isolated or duplicated action execution logic exists (e.g., Overlay and Console use the exact same execution pipeline).
- [ ] No hardcoded dependencies on modern Android APIs without an API 27 (Android 8.1) fallback or compatibility adapter.
- [ ] Code is modular and dependency-light (no Jetpack Compose, no Hilt/Koin, no heavy ML frameworks).

### B. Automated Testing Verification
- [ ] **Unit Tests:** 100% of pure Kotlin domain logic unit tests pass via `./gradlew test`.
- [ ] **Instrumentation Tests:** Android instrumentation tests pass via `./gradlew connectedAndroidTest`.
- [ ] **Negative & Failure Tests:** Tests explicitly verify graceful handling of missing permissions, missing targets, disabled accessibility service, stale nodes, and process death.
- [ ] **Cross-Channel Parity Tests:** Tests confirm that executing an action via Console, Overlay, or Speech produces identical execution logs and state-diff results.

### C. Real-Device Physical Verification (Android 8.1 / API 27 Baseline)
- [ ] Action verified on a physical device or emulator running Android 8.1 (API 27).
- [ ] Action execution proven via observable post-action UI diff (e.g., window changed, text modified, switch checked), not just `performAction() == true`.
- [ ] No UI freezing, ANR (Application Not Responding), or memory leaks during continuous action dispatch.

### D. Logging & Audit Verification
- [ ] Every action dispatch, target resolution step, API result, observation diff, and error produces a structured entry in `logs/agent-events.db`.
- [ ] Log entries include timestamp, correlation ID, subsystem, action type, package/activity, result code, and execution duration.
- [ ] Storage limits and log rotation rules are enforced.

### E. Resource & Low-RAM Verification
- [ ] RAM usage verified during execution; process heap footprint remains within low-RAM bounds (< 50 MB heap).
- [ ] All acquired `AccessibilityNodeInfo` instances are recycled/released immediately after snapshot generation.
- [ ] Idle background state uses ~0% CPU and zero active polling timers.

---

## 3. Phase-Specific Completion Gates

### Phase 1: Foundation & Domain Core
- [ ] `NormalizedCommand` schema finalized and serializable.
- [ ] `CommandRegistry` maps all baseline action types.
- [ ] Thread-safe `GoalDispatcher` unit tests pass with concurrent command dispatch.

### Phase 2: Persistent Storage & Logging
- [ ] SQLite/Room database creates tables correctly on API 27.
- [ ] `EventLogger` writes 1,000 continuous structured logs without dropping events or exceeding storage quotas.
- [ ] Log rotation purges expired records when database reaches size limit.

### Phase 3: Permission & Capability Manager
- [ ] In-app Permission Center correctly reflects status for runtime permissions and special access (Accessibility, Overlay, Usage Access, Write Settings, Notification Listener).
- [ ] Launching system permission settings screens works seamlessly on API 27 through API 36+.

### Phase 4: Accessibility Service Foundation
- [ ] `AgentAccessibilityService` binds successfully on Android 8.1.
- [ ] Service status changes (connected/disconnected) broadcast accurately to `PermissionManager`.

### Phase 5: Observation Engine
- [ ] UI snapshot extraction completes in < 100ms.
- [ ] Live node objects are recycled without memory leaks.
- [ ] `SnapshotDiffEngine` accurately identifies text changes, package changes, and node appearances.

### Phase 6: Global Action Execution
- [ ] `BACK`, `HOME`, `RECENTS` execute reliably via Accessibility API.
- [ ] Post-action verification correctly confirms foreground package changes (e.g., HOME brings launcher to foreground).

### Phase 7: Target Resolution & UI Actions
- [ ] `findClickableAncestor` resolves parent container for non-clickable child TextViews (e.g., Calculator button).
- [ ] `CLICK`, `LONG_CLICK`, `TEXT_INPUT`, and `SCROLL` execute and verify observable UI diffs.
- [ ] Target resolution failure returns explicit error code (`TARGET_NOT_FOUND` / `TARGET_STALE`).

### Phase 8: Application Control Engine
- [ ] `AppResolver` maps app labels (e.g., "Settings", "Calculator") to installed package names on API 27+.
- [ ] `AppLauncher` launches target app and verifies foreground package within 3-second timeout.

### Phase 9: Command Console UI
- [ ] Text commands typed in Console execute via `GoalDispatcher`.
- [ ] Console log view displays real-time execution events from `EventLogger`.

### Phase 10: Movable Overlay Surface
- [ ] Overlay renders using `TYPE_APPLICATION_OVERLAY` on API 27+.
- [ ] Drag gestures move overlay smoothly.
- [ ] Overlay buttons dispatch identical `NormalizedCommand`s as Console and achieve identical execution results.

### Phase 11: Hardware & System Controls
- [ ] Volume and Brightness controls adjust system settings when `WRITE_SETTINGS` is granted.
- [ ] Unsupported hardware actions return `CAPABILITY_UNAVAILABLE` with explicit explanations.

### Phase 12: Speech STT / TTS Subsystem
- [ ] `SpeechRecognizer` captures spoken commands in English, Telugu, Kannada, and Hindi (where available).
- [ ] `TextToSpeech` synthesizes audio responses and releases audio resources immediately when idle.

### Phase 13: Workflow & Automation Engine
- [ ] Sequential multi-step workflows execute deterministically.
- [ ] Step failure triggers configured retry policy or graceful abortion.

### Phase 14: Episodic Memory & Learning
- [ ] Successful action sequences are stored in procedural memory.
- [ ] Replaying learned workflows re-validates live UI targets before executing actions.

### Phase 15: Browser Agent & Web Interaction
- [ ] Browser UI elements observed and interacted with safely.
- [ ] Web page text content isolated from executable command pipeline (prompt injection protection).

### Phase 16: External Knowledge Integration
- [ ] Imported ChatGPT/Gemini export files parsed into structured knowledge entries with source provenance.

### Phase 17: On-Device / Cloud AI Planner
- [ ] AI planner outputs strict JSON plans matching `NormalizedCommand` schemas.
- [ ] Safety policy validator intercepts and blocks unauthorized high-risk commands.

### Phase 18: Autonomous Device Agent
- [ ] End-to-end goal resolution verified: Goal → Observe → Plan → Execute → Verify → Re-plan → Accomplished.

### Phase 19: Low-RAM & Resource Optimization
- [ ] Agent recovers state seamlessly after simulated process termination by Android Low Memory Killer (LMK).
- [ ] Heap allocation remains under 50 MB during heavy UI automation.

### Phase 20: Full Device Certification
- [ ] Master automated test runner executes on physical API 27 hardware with 100% test pass rate for supported capabilities.
