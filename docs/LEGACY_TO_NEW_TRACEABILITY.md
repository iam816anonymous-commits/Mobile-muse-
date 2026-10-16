# Legacy to New Implementation Traceability Matrix (Phases 0–8)

## 1. Overview & Scope Governance

This document establishes the explicit component-by-component mapping between the baseline codebase archived in `legacy/` (67 files from baseline tag `v0.0.1` / commit `1acc7e0`) and the active reconstructed Kotlin implementation in `app/src/` and `core/src/` across Phase 0 through Phase 8.

---

## 2. Phase 0–8 Component Mapping

| Phase | Legacy File (`legacy/`) | Reconstructed Active File (`app/src/` or `core/src/`) | Legacy Behavior & Limitation | Reconstructed Architecture & Improvement | Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **P0** | N/A | `PHASE_PLAN.md`, `docs/PHASE_MAP.md`, `docs/PHASE_SCOPE_GOVERNANCE_RULE.md` | Undocumented phase roadmap rules. | Explicit 23-phase roadmap and Phase Alignment Check rules. | Governance validation in CI |
| **P1** | `legacy/core/.../CommandNormalizer.kt` | `core/.../command/CommandNormalizer.kt` | Monolithic command string splitting. | `CommandNormalizer` returns `NormalizedCommand` sequence with conjunction parsing (`and`, `then`). | `CommandNormalizerTest` |
| **P2** | `legacy/app/.../RoomEventRepository.kt` | `app/.../storage/RoomEventRepository.kt` | Non-deterministic query ordering on duplicate millisecond timestamps. | Deterministic `ORDER BY timestamp DESC, rowid DESC` Room SQLite query. | `RoomEventRepositoryTest` |
| **P3** | `legacy/app/.../PermissionManager.kt` | `app/.../system/PermissionManager.kt` | Direct system permission intent queries without degradation summary. | Centralized permission checker, special access intent builder, passive degradation monitor. | `AppControlEngineTest` |
| **P4** | `legacy/app/.../AgentAccessibilityService.kt` | `app/.../accessibility/AgentAccessibilityService.kt` | Unhandled service unbinding crash. | Passive connection lifecycle binder & connection state monitor. | `AccessibilityServiceConnectionMonitorTest` |
| **P5** | `legacy/app/.../ObservationSnapshotExtractor.kt` | `app/.../accessibility/AgentAccessibilityService.kt` | Singleton global snapshot without `executionId` correlation; stale node reuse. | Live root re-acquisition via `getLiveExternalRootNode(targetPkg)`; attaches `executionId`; atomic generation counter. | `ObservationSnapshotExtractorPhase5Test`, `ObservationSnapshotExtractorTest` |
| **P6** | `legacy/app/.../UiActionExecutor.kt` | `app/.../accessibility/UiActionExecutor.kt` | Forced target package to `lastExternalPackageName`. | Assigns step `executionId` up front; passes `expectedPackageName ?: preSnapshot.packageName`; tags pre/post snapshots. | `ActionExecutionTest` |
| **P7** | `legacy/app/.../CommandExecutionCoordinator.kt` | `app/.../accessibility/CommandExecutionCoordinator.kt` | Single-command lock lacking compound step halting policy. | Sequence execution loop for compound commands; halts on step failure and marks remaining steps `SKIPPED`. | `CommandExecutionCoordinatorTest` |
| **P8** | `legacy/app/.../AppControlEngine.kt` | `app/.../system/AppControlEngine.kt` | Hardcoded launcher package strings (`hilauncher`). | Dynamically queries system Home activity via `pm.resolveActivity(CATEGORY_HOME)`; `LaunchVerifier` system UI resilience. | `AppControlEngineTest` |
