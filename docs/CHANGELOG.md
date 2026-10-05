# CHANGELOG.md — LocalAgent Project Changelog

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
