# DEFINITION_OF_DONE.md — Multi-Tiered Definition of Done Specification

## 1. Overview & Quality Mandate

In **LocalAgent**, a phase or feature is **NEVER** considered complete merely because code compiles or unit tests pass. Because this system interacts directly with the Android platform, UI hierarchy, permissions, and low-RAM hardware, every phase must meet a rigorous, multi-tiered Definition of Done (DoD).

---

## 2. Multi-Tiered Phase Completion Checklist

Before any implementation phase can be marked complete, all of the following criteria must be satisfied across eight explicit validation gates:

### Gate 1: ARCHITECTURE COMPLETE
- [ ] Architecture design adheres strictly to the universal execution pipeline (`NormalizedCommand` → `GoalDispatcher` → `ActionPolicyEngine` → `CapabilityRegistry` → `TargetResolver` → `ActionExecutor` → `VerificationStrategy` → `EventLogger`).
- [ ] Interface boundaries defined with pluggable adapters (no direct tight coupling between UI, speech, AI, or system drivers).
- [ ] All API differences across Android 8.1 (API 27) through Android 15+ (API 36+) documented with fallback adapters.

### Gate 2: CODE COMPLETE
- [ ] Code strictly written in idiomatic, dependency-light Kotlin.
- [ ] No Jetpack Compose, no Hilt/Koin, no permanent ML resident frameworks.
- [ ] Single `rootInActiveWindow` snapshot extraction implemented with explicit `.recycle()` calls in `finally` blocks.
- [ ] Passive degradation logic implemented for unbound accessibility or revoked permissions.

### Gate 3: TIER A UNIT TEST PASS
- [ ] 100% of pure Kotlin domain logic unit tests pass via `./gradlew test`.
- [ ] Command normalization, risk policy evaluation, queue priorities, state-diff logic, and prompt injection tag wrapping verified.

### Gate 4: TIER B CONTRACT & PARITY TEST PASS
- [ ] In-app `TestCenter` suite confirms action contract execution without physical device interaction.
- [ ] Cross-channel parity verified: Dispatching `NormalizedCommand` via Console, Movable Overlay, or Voice produces identical `AgentEvent` traces and execution results.
- [ ] SQLite `agent.db` migrations and WAL file checkpoint accounting verified.

### Gate 5: TIER C DEVICE & E2E TEST PASS
- [ ] Executed and verified on an actual physical device or emulator running Android 8.1 / API level 27 baseline.
- [ ] Action execution proven via action-specific `VerificationStrategy` state diffs (e.g., target text changed, window navigated, checked state toggled), NOT merely `performAction() == true`.
- [ ] E2E interactions verified across real target applications (Settings, Calculator, Files, Chrome Browser).

### Gate 6: OBSERVATION VERIFIED
- [ ] Single-root capture protocol verified; zero secondary calls to `rootInActiveWindow` per snapshot.
- [ ] `NodeIdentityConfidence` assigned to every primitive (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`).
- [ ] Node recycling verified; zero un-recycled `AccessibilityNodeInfo` objects remaining in native C++ heap.

### Gate 7: LOGGING & AUDIT VERIFIED
- [ ] Action dispatch, target resolution, risk policy checks, API results, observation diffs, and errors written to `agent.db`.
- [ ] Correlation IDs cleanly trace multi-step workflows.
- [ ] Total storage footprint (`agent.db` + `agent.db-wal` + `agent.db-shm`) verified within 30 MB cap.

### Gate 8: DOCUMENTATION COMPLETE
- [ ] Source code fully documented with KDoc comments explaining thread safety and lifecycle rules.
- [ ] Phase evidence JSON report generated and saved to `evidence/` directory.
- [ ] `docs/PHASE_[N]_COMPLETION_REPORT.md` written detailing test results, memory profiles, and known limitations.

---

## 3. Explicit Prohibition of "False Success" Claims

The following conditions are **STRICTLY PROHIBITED** from being claimed as completion:

1. **"Build Succeeds" != "Feature Works":** Successful compilation is a prerequisite, not proof of functionality.
2. **"Unit Tests Pass" != "Real Device Execution Works":** Passing mock unit tests does not prove that Android's Accessibility framework or target applications respond correctly on real hardware.
3. **"`performAction() == true`" != "Action Verified":** An Android framework return value of `true` indicates only that the event was dispatched; execution is complete ONLY when `VerificationStrategy` confirms the target UI state diff.
4. **"Console Works" != "System Complete":** A feature is complete ONLY when Console, Overlay, and Voice achieve full execution parity through the universal `GoalDispatcher`.
