# CHANGELOG.md — LocalAgent Project Changelog

## Phase 6 (Completed)
- Implemented Target Resolution domain models (`TargetActionType`, `TargetResolutionStrategy`, `TargetResolutionStatus`, `TargetResolutionResult`) in `:core`.
- Built pure domain `ActionableAncestorResolver` & `TargetResolver` supporting safe parent hierarchy traversal for `CLICK`, `LONG_CLICK`, `SCROLL`, and `EDITABLE` actions up to max depth 30.
- Implemented `LiveTargetResolver` in `:app` for re-acquiring live `AccessibilityNodeInfo` objects from current window hierarchies with live capability re-verification, safe stale target rejection (`TARGET_NOT_FOUND`), and strict recycling lifecycle management (`try/finally`).
- Built `TargetResolutionLogger` with request-scoped deduplication (`beginRequest`/`endRequest`) to log diagnostic resolution events to `agent.db` without database flooding.
- Guaranteed zero action execution (`performAction()`) during target resolution and re-acquisition.
- Created Tier A (`TargetResolverTest.kt`) and Tier B (`LiveTargetResolverTest.kt`) test suites (`P6-RESOLVE-001` through `P6-RESOLVE-010`, `P6-LIVE-001` through `P6-LIVE-006`).

## Phase 5 (Completed)
- Implemented single-root snapshot retrieval contract and immediate `.recycle()` memory management.
- Added `NodeIdentityConfidence` (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`) and bounds `[left,top,right,bottom] (WxH)`.
- Implemented pure domain `SnapshotDiffEngine` in `:core`.
- Added explicit `[ Stop Observation ]` and `[ Clear ]` controls to observation screens.
- Cleaned up duplicate Permission Center buttons across diagnostic screens.

## Phase 4 (Completed)
- Formalized `AccessibilityLifecycleState` and built `AccessibilityServiceConnectionMonitor`.
- Deduplicated candidate rejection logging (`OBSERVATION_EXTERNAL_CANDIDATE_REJECTED`).

## Phase 3 (Completed)
- Built `PermissionManager` and `PermissionActivity` (Permission Center UI).
- Implemented read-only observation and evidence generation with SHA-256 provenance hashing.

## Phase 2 (Completed)
- Implemented Room SQLite `agent.db` event repository and `DurableMemoryStorageManager`.

## Phase 1 (Completed)
- Implemented core domain models, `CommandNormalizer`, `ActionPolicyEngine`, and `GoalDispatcher`.
