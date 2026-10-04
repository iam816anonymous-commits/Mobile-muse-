# PHASE_03_TEST_CASES.md — Phase 3.1 Accessibility Observation Test Cases

## 1. Overview

This document specifies the test cases for Phase 3.1 (Read-Only Accessibility Observation Foundation) of LocalAgent.

---

## 2. Test Cases

### P3-OBS-001
- **Requirement:** Immutable Observation Snapshot & Node Serialization
- **Purpose:** Proves `ObservationNode` and `ObservationSnapshot` preserve hierarchy, package metadata, node attributes, and serialize to deterministic JSON.
- **Preconditions:** Domain model initialized in `:core`.
- **Input:** Construct root node with child node and serialize to JSON string.
- **Expected Result:** JSON string contains node attributes and `fromJsonString()` deserializes identical snapshot.
- **Test Type:** AUTOMATED_JVM
- **Executable Test Location:** `core/src/test/java/com/localagent/core/observation/ObservationDomainModelsTest.kt` -> `testObservationNodeHierarchyAndSerialization()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.observation.ObservationDomainModelsTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P3-OBS-002
- **Requirement:** Observation Traversal Bounds Enforcement (`MAX_NODES = 500`, `MAX_DEPTH = 30`)
- **Purpose:** Proves `ObservationSnapshotExtractor` truncates traversal and sets `isTruncated = true` when node count exceeds 500 or depth exceeds 30.
- **Preconditions:** Extractor initialized.
- **Input:** `extractSnapshot()` called on null or deep/wide node hierarchy.
- **Expected Result:** Snapshot sets `truncationInfo` flag and reason accurately without crashing or hanging.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/accessibility/ObservationSnapshotExtractorTest.kt` -> `testNullRootNodeReturnsEmptySnapshot()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.accessibility.ObservationSnapshotExtractorTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3-OBS-003
- **Requirement:** Read-Only Observation Guarantee (Zero Action Execution)
- **Purpose:** Proves calling `AgentAccessibilityService.captureLiveSnapshot()` observes UI primitives without executing clicks, scrolls, or navigation dispatches.
- **Preconditions:** `AgentAccessibilityService` instantiated in test controller.
- **Input:** Execute `captureLiveSnapshot()`.
- **Expected Result:** Snapshot captured; `GoalDispatcher` queue size remains 0.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt` -> `testObservationIsReadOnlyWithNoActionExecution()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.accessibility.AgentAccessibilityServiceTest"`
- **Permissions:** Simulated Service environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3-UI-002
- **Requirement:** Dedicated Observation Screen Node Tree Rendering
- **Purpose:** Proves `ObservationActivity` formats snapshot nodes into an indented visual tree and displays current and external snapshots separately.
- **Preconditions:** `ObservationActivity` launched in test controller.
- **Input:** Call `renderSnapshotNodeTree()` on `ObservationSnapshot`.
- **Expected Result:** Tree string formats depth indentation (`ROOT`, `├──`, `└──`) and node attributes.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/ui/ObservationActivityTest.kt` -> `testNodeTreeRenderingFormatting()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.ui.ObservationActivityTest"`
- **Permissions:** Simulated UI environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3-DEV-001
- **Requirement:** Physical Phone Accessibility Enablement & Local UI Observation Procedure
- **Purpose:** Verifies that enabling `AgentAccessibilityService` in Android Settings transitions status to `READY (BOUND)` and captures live visible UI hierarchy without performing action execution.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent installed.
- **Input:** Open Settings -> Accessibility -> Enable LocalAgent. Return to app and click "Observe UI".
- **Expected Result:** UI status displays `READY (BOUND)`. Active package, activity, node count, and tree hierarchy displayed. Zero actions executed.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 3)
- **Execution Command:** Manual phone execution
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE` (User grant in Android Accessibility Settings)
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN

### P3-DEV-002
- **Requirement:** Physical Phone External Application Observation Procedure (Chrome/Settings/Calculator/YouTube)
- **Purpose:** Verifies that `AgentAccessibilityService` captures live external application windows and preserves `lastExternalObservationSnapshot` when returning to LocalAgent.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent and Chrome installed.
- **Input:** Open Chrome manually. Switch back to LocalAgent manually and open Observation Screen.
- **Expected Result:** Current Snapshot shows `com.localagent.app` AND Last External Snapshot shows `com.android.chrome`. Indented node tree displays Chrome UI nodes. Zero actions executed.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 3)
- **Execution Command:** Manual phone execution
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE` (User grant in Android Accessibility Settings)
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN
