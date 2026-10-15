# LocalAgent Phase 0 through Phase 8 Rebuild Implementation Plan

## 1. Authoritative Requirements by Authorized Phase (Phases 0–8)

### Phase 0 — Specifications & Planning
- Freeze core architecture specifications and project governance.
- Establish `PHASE_PLAN.md`, `docs/PHASE_MAP.md`, and `docs/PHASE_SCOPE_GOVERNANCE_RULE.md`.
- Perform 100% specification consistency audit.

### Phase 1 — Foundation & Domain Core
- Implement pure Kotlin domain models: `NormalizedCommand`, `CapabilityRegistry`, `ActionPolicyEngine`, `GoalState`, `TaskLifecycle`, priority `GoalDispatcher`.
- Implement unit tests for command parsing, risk policy evaluation, and queue priority.

### Phase 2 — Persistent Storage & Unified Logging
- Implement `agent.db` SQLite Room schema (`agent_events`, `episodes`, `workflows`).
- Implement `EventLogger`, `RoomEventRepository`, `DurableMemoryStorageManager`, WAL checkpointing.

### Phase 3 — Permission & Capability Manager
- Implement `PermissionManager`, API compatibility adapters (API 27–36+), in-app Permission Center UI.
- Handle runtime permissions, special app access, settings intent launching, and passive degradation monitoring.

### Phase 4 — Accessibility Service Foundation
- Implement `AgentAccessibilityService`, passive connection lifecycle binder, and service connection monitor.

### Phase 5 — Observation & Snapshot Engine
- Capture active permitted Android Accessibility node hierarchy.
- Differentiate LocalAgent UI from external target application windows.
- Atomic generation tracking and `executionId` provenance attached to `ObservationSnapshot`.
- Safe live root re-acquisition (`getLiveExternalRootNode`) when accessibility nodes are recycled or detached.

### Phase 6 — Universal Action Engine
- Multi-strategy `TargetResolver` and `ActionableAncestorResolver`.
- Consume typed action requests (`UI_CLICK`, `UI_LONG_CLICK`, `UI_TEXT_INPUT`, `UI_SCROLL_FORWARD`, `UI_SCROLL_BACKWARD`, `GLOBAL_*`, `APP_LAUNCH`).
- Perform pre-action and post-action snapshot capture paired with step `executionId`.
- Evaluate semantic UI state diffs via `TargetAwareVerificationStrategy` and `SnapshotDiffEngine`.

### Phase 7 — Global & UI Action Execution
- Execute framework global actions (`GLOBAL_BACK`, `GLOBAL_HOME`, `GLOBAL_RECENTS`) and UI actions.
- Normalize raw text commands into `NormalizedCommand` sequence via `CommandNormalizer`.
- Orchestrate step execution sequentially via `CommandExecutionCoordinator`, halting compound sequences on step failure and marking downstream steps `SKIPPED`.

### Phase 8 — Application Control Engine
- Dynamically resolve Home activity via `pm.resolveActivity(Intent.ACTION_MAIN + CATEGORY_HOME)`.
- Launch application packages and verify usable foreground window via `LaunchVerifier`.
- Handle transient System UI windows and overlay dialogs without false `PackageMismatch` errors.
- Display exact pre/post snapshot comparison pair in `ObservationActivity` View C matching `executionId`.

---

## 2. CI Pipeline Integration

- `.github/workflows/ci.yml` configured with `push`, `pull_request`, and `workflow_dispatch` triggers.
- Runs repository validation, legacy archive hash check (`sha256sum -c legacy/FILE_HASHES.sha256`), Kotlin compilation, 157 unit/integration tests, and uploads `mobile-muse-debug-apk` artifact.
