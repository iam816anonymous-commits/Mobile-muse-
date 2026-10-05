# Phase 4 Test Report: Accessibility Service Foundation & Candidate Rejection Deduplication

## 1. Executive Summary
The test suite for **Phase 4 — Accessibility Service Foundation** and the **External Candidate Rejection Log Deduplication Correction** was executed across pure Kotlin unit tests (`:core`) and Robolectric framework tests (`:app`) targeting Android API 27 baseline configuration.

All 53 automated test methods passed with a 100% success rate. Android Lint reported zero errors, and `assembleDebug` compiled cleanly.

---

## 2. Test Execution Breakdown

### 2.1 Core Module (`:core`) — Pure Kotlin Domain Tests (20/20 PASS)
- `CommandNormalizerTest`: 8 tests PASS
- `ActionPolicyEngineTest`: 4 tests PASS
- `GoalDispatcherTest`: 4 tests PASS
- `ObservationEvidenceTest`: 4 tests PASS

### 2.2 App Module (`:app`) — Robolectric Framework Tests (33/33 PASS)
- `AccessibilityServiceConnectionMonitorTest`: 3 tests PASS
- `AgentAccessibilityServiceTest`: 10 tests PASS
  - `testUnboundServiceStatus`: PASS
  - `testObservationIsReadOnlyWithNoActionExecution`: PASS (0 dispatches)
  - `testPreservationOfLastExternalObservationSnapshot`: PASS
  - `testValidExternalApplicationPackageClassification`: PASS
  - `testWindowCandidateScoreRanking`: PASS
  - `testCandidateRejectionDeduplicationWithinSingleRequest`: **PASS (Test A — Duplicate SystemUI candidates emitted exactly 1 rejection event)**
  - `testDifferentCandidateRejectionsLoggedIndependently`: **PASS (Test B — SystemUI and Launcher logged as 2 distinct rejection events)**
  - `testDeduplicationResetsOnNewObservationRequest`: **PASS (Test C — Deduplication state resets per request, emitting 2 total events across 2 requests)**
  - `testValidExternalApplicationRemainsFullyObservableAndReadOnly`: **PASS (Test D & E — Calculator remains valid and queue size = 0)**
- `PermissionManagerTest`: 4 tests PASS
- `PermissionActivityTest`: 4 tests PASS
- `ObservationSnapshotExtractorTest`: 4 tests PASS
- `RoomEventRepositoryTest`: 3 tests PASS
- `DurableMemoryStorageManagerTest`: 3 tests PASS
- `MainActivityTest`: 2 tests PASS

---

## 3. Candidate Rejection Log Verification Comparison

### Before Correction
For 1 observation request containing 3 duplicate SystemUI candidates:
- `OBSERVATION_EXTERNAL_CANDIDATE_REJECTED` events = 3 (Excessive log noise)

### After Correction
For 1 observation request containing 3 duplicate SystemUI candidates:
- `OBSERVATION_EXTERNAL_CANDIDATE_REJECTED` events = 1 (Deduplicated)

---

## 4. Physical Device Verification Status

- **Procedure:** Test 4.1 in `PHYSICAL_DEVICE_TEST_PLAN.md`
- **Status:** **NOT_RUN** (Pending execution on physical Android phone hardware)

```text
TEST SUITE SUMMARY:
AUTOMATED TESTS: 53 / 53 PASS (100%)
LINT ANALYSIS: CLEAN (0 ERRORS)
DEBUG APK BUILD: SUCCESS
PHYSICAL DEVICE VERIFICATION: NOT_RUN
```
