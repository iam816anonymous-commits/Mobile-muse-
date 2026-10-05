# Phase 5 Test Cases — Observation & Snapshot Engine

## 1. Executive Summary
This document specifies the executable test suite for **Phase 5 — Observation & Snapshot Engine**.

All test cases verify single-root snapshot retrieval, node identity confidence assignment, Rect bounds computation, pure domain `SnapshotDiffEngine` comparisons, explicit STOP observation controls, immediate `.recycle()` calls, and read-only execution guarantees without performing automated action dispatches or violating phase boundaries.

---

## 2. Test Cases Specification

### Test ID: P5-DIFF-001
- **Requirement:** Pure domain SnapshotDiffEngine comparison (UNCHANGED)
- **Purpose:** Verify identical snapshots produce 0 changes and 1 unchanged diff entry.
- **Preconditions:** Two identical `ObservationSnapshot` instances.
- **Input:** `SnapshotDiffEngine.computeDiff(snapA, snapB)`.
- **Expected Result:** `hasChanges == false`, `totalAdded == 0`, `totalRemoved == 0`, `totalChanged == 0`, `totalUnchanged == 1`.
- **Test Type:** Tier A Pure Kotlin Unit Test
- **Executable Location:** `core/src/test/java/com/localagent/core/observation/SnapshotDiffEngineTest.kt`
- **Execution Command:** `./gradlew :core:test`
- **Permissions:** None
- **Hardware:** Agnostic
- **Evidence:** `SnapshotDiffResult` assertion pass.
- **Status:** PASS

### Test ID: P5-DIFF-002
- **Requirement:** SnapshotDiffEngine added & removed node classification
- **Purpose:** Verify added and removed nodes are correctly classified with `ADDED` and `REMOVED` diff types.
- **Preconditions:** Snapshot A contains Item 1; Snapshot B contains Item 2.
- **Input:** `SnapshotDiffEngine.computeDiff(snapA, snapB)`.
- **Expected Result:** `totalAdded == 1`, `totalRemoved == 1`, `totalUnchanged == 1`.
- **Test Type:** Tier A Pure Kotlin Unit Test
- **Executable Location:** `core/src/test/java/com/localagent/core/observation/SnapshotDiffEngineTest.kt`
- **Execution Command:** `./gradlew :core:test`
- **Permissions:** None
- **Hardware:** Agnostic
- **Evidence:** `SnapshotDiffResult` entries matching `NodeDiffType.ADDED` and `NodeDiffType.REMOVED`.
- **Status:** PASS

### Test ID: P5-DIFF-003
- **Requirement:** SnapshotDiffEngine changed attribute detection
- **Purpose:** Verify modifying node state (e.g. `enabled` toggled false) is detected as `CHANGED` with `changedAttributes` listing `"enabled"`.
- **Preconditions:** Snapshot A button enabled = true; Snapshot B button enabled = false.
- **Input:** `SnapshotDiffEngine.computeDiff(snapA, snapB)`.
- **Expected Result:** `totalChanged == 1`, `changedAttributes` contains `"enabled"`.
- **Test Type:** Tier A Pure Kotlin Unit Test
- **Executable Location:** `core/src/test/java/com/localagent/core/observation/SnapshotDiffEngineTest.kt`
- **Execution Command:** `./gradlew :core:test`
- **Permissions:** None
- **Hardware:** Agnostic
- **Evidence:** `NodeDiffEntry.changedAttributes` contains `"enabled"`.
- **Status:** PASS

### Test ID: P5-DIFF-004
- **Requirement:** SnapshotDiffEngine null & empty snapshot safety
- **Purpose:** Verify comparing null or empty snapshots handles safely without throwing exceptions.
- **Preconditions:** Null `beforeSnapshot` and null `afterSnapshot`.
- **Input:** `SnapshotDiffEngine.computeDiff(null, null)`.
- **Expected Result:** Returns valid empty `SnapshotDiffResult` with `hasChanges == false`.
- **Test Type:** Tier A Pure Kotlin Unit Test
- **Executable Location:** `core/src/test/java/com/localagent/core/observation/SnapshotDiffEngineTest.kt`
- **Execution Command:** `./gradlew :core:test`
- **Permissions:** None
- **Hardware:** Agnostic
- **Evidence:** Valid `SnapshotDiffResult` with 0 diff entries.
- **Status:** PASS

### Test ID: P5-SNAP-001
- **Requirement:** Single-root snapshot retrieval & bounds extraction
- **Purpose:** Verify snapshot extraction executes exactly one root retrieval and extracts `ObservationBounds` (`left`, `top`, `right`, `bottom`, `width`, `height`).
- **Preconditions:** `ObservationSnapshotExtractor` initialized.
- **Input:** `extractSnapshot(rootNodeInfo, pkg, windowId)`.
- **Expected Result:** Extracted snapshot contains bounds with valid `width` and `height`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/ObservationSnapshotExtractorPhase5Test.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `ObservationBounds` assertion pass.
- **Status:** PASS

### Test ID: P5-SNAP-002
- **Requirement:** Node identity and NodeIdentityConfidence assignment
- **Purpose:** Verify `computeIdentity()` assigns appropriate `NodeIdentityConfidence` levels (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`).
- **Preconditions:** Various node attribute combinations (resource ID, text, parent identity).
- **Input:** `computeIdentity()` with test attributes.
- **Expected Result:** Resource ID + text -> `EXACT`; Resource ID alone -> `HIGH`; Text alone -> `MEDIUM`; Parent identity alone -> `LOW`; Neither -> `EPHEMERAL`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/ObservationSnapshotExtractorPhase5Test.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `NodeIdentityConfidence` enum value matching expected level.
- **Status:** PASS

### Test ID: P5-RECYC-001
- **Requirement:** Immediate AccessibilityNodeInfo recycling
- **Purpose:** Verify all acquired `AccessibilityNodeInfo` instances (root, children, truncated branches) are recycled in `finally` blocks during snapshot extraction.
- **Preconditions:** Tree traversal with `ObservationSnapshotExtractor`.
- **Input:** Snapshot extraction over mock/framework node hierarchy.
- **Expected Result:** Zero un-recycled native nodes remaining.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/ObservationSnapshotExtractorPhase5Test.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Traversal `finally { childNode.recycle() }` assertion.
- **Status:** PASS

### Test ID: P5-CTRL-001
- **Requirement:** Explicit STOP observation controls & state preservation
- **Purpose:** Verify `btnStopCurrentObservation` and `btnStopExternalObservation` transition engine state to `STOPPED`, log `OBSERVATION_STOPPED`, preserve last snapshot, and leave Accessibility Service running.
- **Preconditions:** Observation activity launched.
- **Input:** Click "Stop Observation".
- **Expected Result:** Engine state = `STOPPED`, Toast shown, last snapshot preserved, `AgentAccessibilityService.isBound` remains unchanged.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Event log entry `OBSERVATION_STOPPED`.
- **Status:** PASS

### Test ID: P5-READ-001
- **Requirement:** Phase 5 Read-Only Guarantee
- **Purpose:** Confirm snapshot generation and diff engine execution produce exactly 0 action dispatches against observed applications.
- **Preconditions:** Active observation cycle.
- **Input:** `captureLiveSnapshot()` + `SnapshotDiffEngine.computeDiff()`.
- **Expected Result:** GoalDispatcher queue size = 0. Zero clicks, scrolls, or inputs.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `app.goalDispatcher.getQueueSize() == 0`.
- **Status:** PASS
