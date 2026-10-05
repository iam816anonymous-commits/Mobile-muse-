# PHASE_SCOPE_GOVERNANCE_RULE.md — LocalAgent Permanent Phase Scope Governance Rule

## 1. Executive Summary

This document establishes the **Permanent Phase Scope Governance Rule** for the LocalAgent repository.

Before implementing ANY requirement, feature, test, UI screen, subsystem, refactor, dependency, or architectural change in LocalAgent, developers and automated agents MUST perform an explicit **Phase Alignment Check** against the authoritative 23-phase roadmap (`PHASE_PLAN.md`) and the current phase specification.

This is a mandatory project governance rule. Functionality from another phase must NEVER be silently pulled forward or merged into the current phase.

---

## 2. Authoritative Phase Rule

The frozen 23-phase roadmap in `PHASE_PLAN.md` and the specification for the **CURRENT PHASE** are the single source of truth for project phase scope.

Before beginning implementation work on any task:

1. Identify the current phase.
2. Read its authoritative phase specification.
3. Identify the requested feature or change.
4. Determine whether the feature explicitly belongs to the current phase.
5. Check whether it is an existing dependency from an earlier phase.
6. Check whether it belongs to a later/future phase.
7. Check whether implementing it would create architectural scope creep.

A feature does NOT belong to the current phase merely because it is technically useful, convenient, or required by a later subsystem.

---

## 3. Mandatory Classification Tiers

Every proposed feature or change must be classified as exactly one of the following four tiers:

### 🟢 IN PHASE
- **Definition:** The feature is explicitly required by the authoritative specification of the current phase.
- **Action:** Implement it according to contract specifications and DoD requirements.

### 🟡 EXISTING DEPENDENCY
- **Definition:** The feature belongs to an earlier phase and has already been implemented and verified.
- **Action:** Reuse the existing subsystem/API/contract. Do NOT duplicate, bypass, or redesign it unless the current phase explicitly requires a documented contract extension.

### 🔵 FUTURE PHASE
- **Definition:** The feature belongs to a later phase or a separate subsystem whose official implementation is scheduled later in the roadmap.
- **Action:** Do NOT implement it in the current phase. Report:
  > `"FUTURE-PHASE FEATURE — NOT IMPLEMENTED IN CURRENT PHASE."`
  Record the reference/dependency for when its official phase arrives.

### 🔴 SCOPE VIOLATION
- **Definition:** The requested change is not justified by the current phase and would introduce unrelated functionality, architectural coupling, or premature implementation.
- **Action:** Do NOT implement it. Report:
  > `"PHASE SCOPE VIOLATION — IMPLEMENTATION BLOCKED."`

---

## 4. Stop Before Implementation Protocol

If a requested feature is classified as 🔵 **FUTURE PHASE** or 🔴 **SCOPE VIOLATION**, STOP immediately before modifying source code.

Do NOT:
- Implement it partially or as a "quick proof of concept";
- Create placeholder classes or stubs for the future feature;
- Create tests for the future feature;
- Create UI elements or layout buttons for the future feature;
- Silently add Gradle or library dependencies for it;
- Rename or redefine the current phase to accommodate it;
- Modify the 23-phase roadmap map to make the feature appear in-scope.

Instead, report the classification and wait for explicit project owner approval.

---

## 5. Explicit User Decision Required Protocol

If a feature is useful but belongs to another phase, present it explicitly to the project owner before proceeding:

```text
FEATURE:
<feature name and description>

CURRENT PHASE:
<current phase number and title>

CLASSIFICATION:
🔵 FUTURE PHASE

ORIGIN:
<phase/subsystem where it belongs on the roadmap>

REASON:
<why it is outside the current phase scope>

RECOMMENDATION:
Keep it in its original phase unless there is a strong architectural reason to pull it forward.

DECISION REQUIRED:
- Keep for original phase
- Explicitly pull into current phase
- Reject
```

Never make this phase-pull decision silently or implicitly.

---

## 6. Special Rule for Existing Early Implementations

LocalAgent may contain functionality that was implemented earlier than its official roadmap phase (for example, early prototype code or foundational observation experiments).

### Example: Existing Observation Subsystem
The Accessibility Observation Foundation currently contains:
- `AgentAccessibilityService`
- `ObservationSnapshot` & `ObservationNode`
- `ObservationSnapshotExtractor`
- Current UI observation
- External application observation
- External snapshot preservation
- `ObservationActivity`
- Indented node-tree rendering

These existing components MUST NOT automatically redefine the official phase roadmap.

Treat existing components as **existing/early implementation** unless the authoritative roadmap (`PHASE_PLAN.md`) is explicitly modified by the project owner. The existence of code in the repository is NOT proof that the current phase owns that functionality.

---

## 7. Observation Subsystem Scope Rule

The Accessibility Observation Foundation is a separate, read-only subsystem.

Its scope is strictly bounded to:
- Current UI observation
- External application observation
- Accessibility node hierarchy extraction
- Bounded observation snapshots (`MAX_NODES=500`, `MAX_DEPTH=30`)
- Indented node-tree rendering
- External snapshot preservation
- Read-only observation with zero action execution

Do NOT expand this subsystem into:
- Automatic clicking
- Scrolling external applications
- Typing / text entry
- Autonomous navigation
- Target resolution
- Workflow execution
- AI decision making
- Autonomous learning

unless the authoritative phase currently being implemented explicitly requires those capabilities.

---

## 8. UI Scope Rule

Do NOT place unrelated functionality onto an existing screen merely because it is convenient or easy. Use dedicated screens when a subsystem has a distinct responsibility.

### Mandatory 4-Screen Separation
- **`MainActivity`** → Home / System Status Dashboard
- **`ObservationActivity`** → UI Observation / Node Trees / Window Candidate Diagnostics
- **`EventLogActivity`** → Persistent Event History (`agent.db` stream)
- **`StorageDiagnosticsActivity`** → Storage & Durable Long-Term Memory Diagnostics
- *(Future)* **`PermissionActivity`** → Permission & Capability Management Center

Do NOT merge these screen responsibilities without explicit architectural approval.

---

## 9. Test Scope Rule

Tests must follow the same strict phase boundary as production code.

A test does NOT belong to the current phase merely because it is easy to execute or write. Before adding a test, classify the requirement being tested.

### Example: Phase 3
- Permission status test → **IN PHASE**
- Settings intent launch test → **IN PHASE**
- Accessibility degradation test → **IN PHASE**
- Chrome node-tree extraction test → **OBSERVATION SUBSYSTEM (Not automatically Phase 3)**

Never create future-phase tests merely to artificially increase the current phase's test count.

---

## 10. Documentation Rule

Phase reports, completion reports, test reports, traceability matrices, and architecture documents must preserve clear distinctions between:
- Current-phase functionality
- Existing dependencies
- Early implementations
- Future-phase functionality
- Physical-device verification (`NOT_RUN` until executed on real hardware)
- Automated verification (`PASS` via JVM/Robolectric)

Do NOT claim that a future-phase subsystem is complete merely because some of its code already exists. Do NOT rename or reinterpret the 23-phase roadmap to match implementation drift.

---

## 11. Required Pre-Implementation Alignment Check

Before beginning substantial implementation work for any phase, produce a **PHASE ALIGNMENT CHECK**:

```markdown
# PHASE ALIGNMENT CHECK

Current Phase:
<phase number and title>

Authoritative Requirements:
<list requirements from PHASE_PLAN.md>

Requested Changes:
<list requested features/changes>

Classification:

| Requested Change | Classification | Phase Owner | Action |
|---|---|---|---|
| Requirement A | 🟢 IN PHASE | Current Phase | Implement |
| Requirement B | 🟡 EXISTING DEPENDENCY | Earlier Phase | Reuse |
| Feature C | 🔵 FUTURE PHASE | Phase X | Do NOT implement |
| Feature D | 🔴 SCOPE VIOLATION | None | Reject |
```

Proceed ONLY with items classified as 🟢 **IN PHASE** or 🟡 **EXISTING DEPENDENCY**. Items classified 🔵 or 🔴 must NOT be implemented without explicit project owner approval.

---

## 12. Phase Completion Rule

A phase may ONLY be marked **COMPLETE** when:

1. Its authoritative requirements from `PHASE_PLAN.md` are fully implemented.
2. Its required tests are implemented and passing.
3. Its required evidence is produced.
4. Its exit criteria are satisfied.
5. No unapproved future-phase functionality has been silently included.
6. Any early implementation belonging to another phase is clearly documented as such.
7. Automated tests, lint, and assembleDebug build pass cleanly (`./gradlew clean test lint assembleDebug`).
8. Physical-device requirements are clearly distinguished from automated tests (marked `NOT_RUN` until physically executed).

"Code exists" is NOT sufficient evidence that a phase requirement is complete.

---

## 13. Summary

Never optimize for "making the app look more complete" by implementing future phases early.

The core objective is:

**CORRECT PHASE → CORRECT SCOPE → CORRECT IMPLEMENTATION → CORRECT TESTS → CORRECT EVIDENCE.**

Not:

*Implement everything that looks useful.*
