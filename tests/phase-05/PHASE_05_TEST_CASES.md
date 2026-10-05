# Phase 5 Test Cases — Observation & Snapshot Engine (Phase 5 Correction)

## 1. Executive Summary
This document specifies the executable test suite for **Phase 5 — Observation & Snapshot Engine** including the Phase 5 Correction for Observation UI Cleanup, Clear Controls, Node/Bounds Validation, and Tree Duplication Fix.

All test cases verify single-root snapshot retrieval, node identity confidence assignment, Rect bounds computation and display (`[left,top,right,bottom]` and `WxH`), pure domain `SnapshotDiffEngine` comparisons, explicit STOP and CLEAR observation controls, immediate `.recycle()` calls, absence of direct Permission Center navigation buttons on observation screens, and read-only execution guarantees without performing automated action dispatches or violating phase boundaries.

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

### Test ID: P5-UI-NAV-001 (Tests A, B, C)
- **Requirement:** Removal of direct Permission Center buttons from observation/evidence screens
- **Purpose:** Verify Permission Center buttons are absent from `CurrentObservationActivity`, `ExternalObservationActivity`, and `EvidenceActivity`.
- **Preconditions:** Observation and evidence Activities inflated.
- **Input:** Inspect view layout.
- **Expected Result:** `btnOpenPermissionCenter` is null on all three screens.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt`, `ExternalObservationActivityTest.kt`, `EvidenceActivityTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `assertNull(permBtn)` pass.
- **Status:** PASS

### Test ID: P5-UI-CLR-001 (Tests D & E)
- **Requirement:** Clear button removes displayed snapshot without unbinding service
- **Purpose:** Verify tapping Clear on Current or External observation screen removes displayed node tree and resets to empty state ("No current/external observation captured.") while keeping AccessibilityService bound.
- **Preconditions:** Observation Activity displayed.
- **Input:** Tap "Clear".
- **Expected Result:** Screen tree resets to empty state; `AgentAccessibilityService.isBound` remains unchanged; `agent.db` records remain un-modified.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt`, `ExternalObservationActivityTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Tree text equals `"No current observation captured."` or `"No external observation captured."`
- **Status:** PASS

### Test ID: P5-UI-DUP-001 through P5-UI-DUP-004
- **Requirement:** Tree rendering replacement & single tree header guarantee
- **Purpose:** Verify 1 Observe request renders 1 tree, 2 consecutive Observe requests replace the previous tree, Clear removes displayed tree, and Observe after Clear renders 1 tree.
- **Preconditions:** Active observation screen.
- **Input:** Observe / Clear button interactions.
- **Expected Result:** Tree text contains exactly 1 `"ROOT ["` header occurrence; consecutive Observe clicks replace previous tree.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Root occurrences assertion = 1.
- **Status:** PASS

### Test ID: P5-UI-BND-001 (Tests F & G)
- **Requirement:** Bounds display on node tree rendering
- **Purpose:** Verify rendered node tree string explicitly contains `bounds:[left,top,right,bottom]` and size `(WxH)`.
- **Preconditions:** Node with `ObservationBounds(left=10, top=20, right=110, bottom=70)`.
- **Input:** `renderSnapshotNodeTree(snapshot)`.
- **Expected Result:** String contains `"bounds:[10,20,110,70] (100x50)"`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt`, `ExternalObservationActivityTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `treeStr.contains("bounds:[10,20,110,70] (100x50)")` pass.
- **Status:** PASS

### Test ID: P5-UI-DUP-005 (Tests H & I)
- **Requirement:** Node tree rendering uniqueness & external app snapshot isolation
- **Purpose:** Verify each domain `ObservationNode` is rendered exactly once without duplicate text lines, and external app snapshots contain 0 LocalAgent nodes.
- **Preconditions:** Snapshot containing single node "Unique Title Text" or external FrameLayout.
- **Input:** `renderSnapshotNodeTree(snapshot)`.
- **Expected Result:** Node string occurs exactly once; external snapshot contains `ROOT [com.android.calculator2]` and 0 `com.localagent.app` nodes.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt`, `ExternalObservationActivityTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Occurrences count = 1; `assertFalse(treeStr.contains("com.localagent.app"))`.
- **Status:** PASS

### Test ID: P5-READ-001 (Tests J, K, L)
- **Requirement:** Single-root semantics, immediate node recycling & read-only guarantee
- **Purpose:** Verify single root retrieval per cycle, immediate `.recycle()` calls in `finally` blocks, and 0 action dispatches.
- **Preconditions:** Active observation cycle.
- **Input:** `captureLiveSnapshot()`.
- **Expected Result:** GoalDispatcher queue size = 0. Zero dispatches performed.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt`
- **Execution Command:** `./gradlew :app:testDebugUnitTest`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Queue size = 0; traversal `.recycle()` pass.
- **Status:** PASS
