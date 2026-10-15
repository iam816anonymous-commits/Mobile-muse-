# Legacy to New Implementation Traceability Matrix

## 1. Overview & Knowledge Transfer Governance

This document establishes the explicit component-by-component and responsibility-by-responsibility mapping between the baseline codebase archived in `legacy/` (from baseline tag `v0.0.1` / commit `1acc7e0`) and the active reconstructed Kotlin implementation in `app/src/` and `core/src/`.

---

## 2. Component Traceability & Architectural Shift Mapping

| Baseline Legacy File (`legacy/`) | Reconstructed Active File (`app/src/` or `core/src/`) | Legacy Responsibility & Weaknesses | New Architectural Replacement & Improvements | Preserved Required Capabilities |
| :--- | :--- | :--- | :--- | :--- |
| `legacy/app/.../AgentAccessibilityService.kt` | `app/.../AgentAccessibilityService.kt` | Singleton global snapshot without execution ID tracking; stale node reuse when views recycle. | Live root re-acquisition via `getLiveExternalRootNode(targetPkg)`; tags snapshot with `executionId`; scoring (170 pts) prioritizes focused external apps. | Candidate window collection, external package filtering, event-driven snapshot publication. |
| `legacy/app/.../UiActionExecutor.kt` | `app/.../UiActionExecutor.kt` | Forced target package to `lastExternalPackageName`; unverified action dispatch. | Assigns step `executionId` up front; passes `expectedPackageName ?: preSnapshot.packageName`; tags pre/post snapshot pair with `executionId`. | Framework `performAction` dispatch, touch gesture fallback, scroll container detection. |
| `legacy/app/.../CommandExecutionCoordinator.kt` | `app/.../CommandExecutionCoordinator.kt` | Monolithic handler lacking compound step execution context or failure halting. | Sequence execution loop for compound commands (`and`, `then`); halts on step failure and marks remaining steps `SKIPPED`. | Command request mapping, global/UI/observe action dispatch routing. |
| `legacy/app/.../AppControlEngine.kt` | `app/.../AppControlEngine.kt` | Hardcoded launcher package strings (`hilauncher`). | Dynamically queries system Home activity via `pm.resolveActivity(CATEGORY_HOME)`; `LaunchVerifier` system UI resilience. | Intent package launch, foreground window waiting loop. |
| `legacy/app/.../ObservationActivity.kt` | `app/.../ObservationActivity.kt` | Global latest snapshot fallback across tabs. | View C binds Panel A (preSnapshot) and Panel B (postSnapshot) strictly matching `executionId` from history; displays build tag `[v0.0.1-P8-SNAPSHOT-CAPT]`. | View A live screen node tree, View B last external screen, View C comparison pair. |
| `legacy/core/.../ActionDomainModels.kt` | `core/.../ActionDomainModels.kt` | Simple request/result models without provenance IDs or diagnostic failure stages. | Binds `executionId`, `ActionType.APP_LAUNCH`, explicit `FailureStage` enum, detailed diagnostic reason messages. | Action request parameters, verification status codes. |
| `legacy/core/.../VerificationStrategies.kt` | `core/.../VerificationStrategies.kt` | Generation counter alone treated as success proof. | `TargetAwareVerificationStrategy` computes semantic diffs via `SnapshotDiffEngine`, restricts `TARGET_STALE` checks strictly to resource ID targets. | Semantic state change evaluation (`diffResult.hasChanges`). |
| `legacy/core/.../ObservationDomainModels.kt` | `core/.../ObservationDomainModels.kt` | Child index in node key caused false diffs when layout shifted. | Stable identity key (`id:resourceId` without child index); `ObservationSnapshot` contains `executionId: String? = null`. | Bounded geometry, text/contentDescription properties, tree traversal. |

---

## 3. Phase Knowledge Transfer Gates

1. **Phase 5 (Observation Gate):** Mapped legacy `AgentAccessibilityService.kt` and `ObservationSnapshotExtractor.kt` to new live root re-acquisition and `executionId` provenance models.
2. **Phase 6 (UI Action Gate):** Mapped legacy `UiActionExecutor.kt` and `VerificationStrategies.kt` to action-scoped pre/post snapshot capture and semantic diff verification.
3. **Phase 7 (Automation Gate):** Mapped legacy `CommandExecutionCoordinator.kt` to compound command sequence execution, step execution IDs, and failure halting policies.
4. **Phase 8 (App Control Gate):** Mapped legacy `AppControlEngine.kt` to dynamic Home package resolution via `PackageManager` and exact foreground launch verification.
