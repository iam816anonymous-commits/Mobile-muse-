# PHASE_03_TEST_CASES.md — Phase 3 Test Cases (Phases 3.1, 3.2, 3.3)

## 1. Overview

This document specifies the test cases for Phase 3.1 (Read-Only Accessibility Observation Foundation), Phase 3.2 (Evidence), and Phase 3.3 (Permission & Capability Manager) of LocalAgent.

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
- **Requirement:** Current UI Observation Screen & Node Tree Rendering
- **Purpose:** Proves `CurrentObservationActivity` formats snapshot nodes into an indented visual tree and renders current UI snapshot metadata cleanly.
- **Preconditions:** `CurrentObservationActivity` launched in test controller.
- **Input:** Call `renderSnapshotNodeTree()` on `ObservationSnapshot`.
- **Expected Result:** Tree string formats depth indentation (`ROOT`, `├──`, `└──`) and node attributes.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/ui/CurrentObservationActivityTest.kt` -> `testCurrentNodeTreeRendering()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.ui.CurrentObservationActivityTest"`
- **Permissions:** Simulated UI environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3-UI-003
- **Requirement:** External Application Observation Screen & Valid Target Diagnostics
- **Purpose:** Proves `ExternalObservationActivity` formats valid external application snapshot nodes, displays target validation diagnostics, and isolates external observation from current UI.
- **Preconditions:** `ExternalObservationActivity` launched in test controller.
- **Input:** Call `renderSnapshotNodeTree()` on external `ObservationSnapshot`.
- **Expected Result:** Tree string formats depth indentation and displays valid target package diagnostics.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/ui/ExternalObservationActivityTest.kt` -> `testExternalNodeTreeRendering()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.ui.ExternalObservationActivityTest"`
- **Permissions:** Simulated UI environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3.2-EVID-001
- **Requirement:** Observation Evidence Generation & SHA-256 Provenance Hash Calculation
- **Purpose:** Proves `ObservationEvidence` is generated deterministically from `ObservationSnapshot`, computes SHA-256 provenance hash over primitives, and serializes to valid JSON string.
- **Preconditions:** Observation snapshot initialized.
- **Input:** Call `ObservationEvidence.fromSnapshot()`.
- **Expected Result:** Evidence object created with matching package metadata, snapshot ID, and SHA-256 provenance hash.
- **Test Type:** AUTOMATED_JVM
- **Executable Test Location:** `core/src/test/java/com/localagent/core/evidence/EvidenceDomainModelsTest.kt` -> `testObservationEvidenceFromSnapshotAndProvenanceHash()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.evidence.EvidenceDomainModelsTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P3.2-UI-001
- **Requirement:** Dedicated Evidence Screen & Primitive Stream Rendering
- **Purpose:** Proves `EvidenceActivity` formats Current and External Evidence primitives into scrollable text streams, displaying SHA-256 provenance hashes and snapshot traceability metadata.
- **Preconditions:** `EvidenceActivity` launched in test controller.
- **Input:** Call `renderEvidencePrimitives()` and `formatMetaText()`.
- **Expected Result:** UI renders evidence metadata, SHA-256 provenance hash, and primitive stream in dual scrollable views.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/ui/EvidenceActivityTest.kt` -> `testEvidencePrimitivesRendering()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.ui.EvidenceActivityTest"`
- **Permissions:** Simulated UI environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3.3-PERM-001
- **Requirement:** Permission Status Checking & Settings Intent Generation
- **Purpose:** Proves `PermissionManager` checks runtime and special access permissions, generates correct Android Settings Intents (`ACTION_ACCESSIBILITY_SETTINGS`, `ACTION_MANAGE_OVERLAY_PERMISSION`, `ACTION_MANAGE_WRITE_SETTINGS`, `ACTION_USAGE_ACCESS_SETTINGS`), and generates SAF Document Picker intent.
- **Preconditions:** `PermissionManager` instantiated.
- **Input:** Execute `getAllPermissions()`, `getSettingsIntentForPermission()`, `getSafDocumentPickerIntent()`.
- **Expected Result:** Permissions list returned, settings intents generated with matching actions, SAF picker intent configured.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/system/PermissionManagerTest.kt` -> `testGetAllPermissionsList()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.system.PermissionManagerTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3.3-UI-001
- **Requirement:** Dedicated Permission Center UI Screen
- **Purpose:** Proves `PermissionActivity` renders special access cards, runtime permission status, passive degradation summaries, and settings launch triggers.
- **Preconditions:** `PermissionActivity` launched in test controller.
- **Input:** Launch activity via Robolectric.
- **Expected Result:** UI summary text and 7 permission cards rendered cleanly.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/ui/PermissionActivityTest.kt` -> `testPermissionActivityLaunchAndSummary()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.ui.PermissionActivityTest"`
- **Permissions:** Simulated UI environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P3-DEV-001
- **Requirement:** Physical Phone Accessibility Enablement & Local UI Observation Procedure
- **Purpose:** Verifies that enabling `AgentAccessibilityService` in Android Settings transitions status to `READY (BOUND)` and captures live visible UI hierarchy without performing action execution.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent installed.
- **Input:** Open Settings -> Accessibility -> Enable LocalAgent. Return to app and click "Current UI Observation".
- **Expected Result:** UI status displays `READY (BOUND)`. Active package, activity, node count, and tree hierarchy displayed. Zero actions executed.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 3)
- **Execution Command:** Manual phone execution
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE` (User grant in Android Accessibility Settings)
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN

### P3.1-DEV-EXT-001
- **Requirement:** Physical Phone External Chrome + Recents Preservation Procedure
- **Purpose:** Verifies that `AgentAccessibilityService` captures live Google Chrome interactive windows, rejects `com.android.systemui` Recents windows as valid external candidates, and preserves `lastExternalObservationSnapshot` (`com.android.chrome`) when returning to LocalAgent or opening Recents.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent and Chrome installed.
- **Input:** Open Chrome manually. Open Recents manually. Return to LocalAgent and open `ExternalObservationActivity`.
- **Expected Result:** Validated target is `com.android.chrome`. Indented Chrome node tree displayed in dual scrollable layout. Recents (`com.android.systemui`) does NOT overwrite Chrome snapshot. Zero actions executed automatically.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 3.3)
- **Execution Command:** Manual phone execution
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE` (User grant in Android Accessibility Settings)
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN

### P3.2-DEV-EVID-001
- **Requirement:** Physical Phone Observation Evidence Generation & Provenance Hash Procedure
- **Purpose:** Verifies that `AgentAccessibilityService` generates `ObservationEvidence` with SHA-256 provenance hash from live observation snapshots without executing UI actions.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent installed and Accessibility enabled.
- **Input:** Open Chrome, return to LocalAgent, open `EvidenceActivity`, and click "Capture Live Evidence".
- **Expected Result:** Provenance hash, evidence ID, snapshot ID, and evidence primitive stream rendered cleanly. Zero actions executed against observed apps.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 3.4)
- **Execution Command:** Manual phone execution
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE`
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN

### P3.3-DEV-PERM-001
- **Requirement:** Physical Phone Permission Center & Settings Intent Launch Procedure
- **Purpose:** Verifies that Permission Center UI correctly inspects physical phone permission states and launches system settings intent flows for overlay, write settings, accessibility, usage access, and SAF document picker.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent installed.
- **Input:** Launch `PermissionActivity` and tap setting intent launch buttons.
- **Expected Result:** Target Android System Settings screens launch cleanly. SAF document picker opens dialog.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 3.5)
- **Execution Command:** Manual phone execution
- **Permissions:** Special Access & Runtime Permissions
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN
