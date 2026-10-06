# TEST_TRACEABILITY_MATRIX.md — Master Requirement to Test Traceability Matrix

## 1. Executive Summary

This matrix maps every requirement across frozen phases (Phase 0, Phase 1, Phase 2, Phase 3, Phase 4, Phase 5, Phase 6) to its exact stable Test ID, test classification type, executable source location, execution command, permissions, hardware, and verification status.

Every requirement must map to an automated JVM test, Robolectric test, instrumentation test, physical-device test procedure, or specification audit.

---

## 2. Phase 0 — Specification & Planning Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Single command execution pipeline spec | P0-SPEC-001 | MANUAL | `ARCHITECTURE.md` Audit | `cat ARCHITECTURE.md` | PASS |
| 23-phase implementation roadmap | P0-SPEC-002 | MANUAL | `PHASE_PLAN.md` Audit | `cat PHASE_PLAN.md` | PASS |

---

## 3. Phase 1 — Foundation & Domain Core Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Canonical command grammar parsing | P1-CMD-001 | AUTOMATED_JVM | `CommandNormalizerTest.kt` | `./gradlew :core:test` | PASS |
| Unknown command rejection at normalizer | P1-CMD-002 | AUTOMATED_JVM | `CommandNormalizerTest.kt` | `./gradlew :core:test` | PASS |
| Empty / whitespace input rejection | P1-CMD-003 | AUTOMATED_JVM | `CommandNormalizerTest.kt` | `./gradlew :core:test` | PASS |
| Action Policy Engine risk evaluation | P1-POL-001 | AUTOMATED_JVM | `ActionPolicyEngineTest.kt` | `./gradlew :core:test` | PASS |
| Priority queue & execution locking | P1-QUEUE-001 | AUTOMATED_JVM | `GoalDispatcherTest.kt` | `./gradlew :core:test` | PASS |
| Task lifecycle & LMK recovery state | P1-LIFE-001 | AUTOMATED_JVM | `TaskLifecycleTest.kt` | `./gradlew :core:test` | PASS |

---

## 4. Phase 2 — Persistent Storage & Unified Logging Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Event persistence in Room `agent.db` | P2-LOG-001 | AUTOMATED_ROBOLECTRIC | `RoomEventRepositoryTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Storage footprint pruning & retention cap | P2-CAP-001 | AUTOMATED_ROBOLECTRIC | `RoomEventRepositoryTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Sensitive metadata JSON sanitization | P2-SEC-001 | AUTOMATED_ROBOLECTRIC | `RoomEventRepositoryTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Diagnostic UI event stream DB hydration | P2-UI-001 | AUTOMATED_ROBOLECTRIC | `MainActivityEventHydrationTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Durable memory record SHA-256 integrity | P2-MEM-001 | AUTOMATED_ROBOLECTRIC | `DurableMemoryStorageManagerTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Durable memory app-private deletion simulation | P2-MEM-002 | AUTOMATED_ROBOLECTRIC_SIMULATION | `DurableMemoryStorageManagerTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS (Simulated) |
| Physical phone uninstall survival procedure | P2-DEV-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 2 | Manual Installation on Physical Phone | **NOT_RUN** |

---

## 5. Phase 3 — Permission Center & UI Consolidation Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Centralized permission categorization | P3.3-PERM-001 | AUTOMATED_ROBOLECTRIC | `PermissionManagerTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Permission Center UI & UI Consolidation | P3.3-UI-001 | AUTOMATED_ROBOLECTRIC | `PermissionActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Physical phone Permission Center procedure | P3.3-DEV-PERM-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 3.5 | Manual Permission Center on Physical Phone | **NOT_RUN** |

---

## 6. Phase 4 — Accessibility Service Foundation Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| AgentAccessibilityService binding lifecycle monitoring | P4-ACC-001 | AUTOMATED_ROBOLECTRIC | `AccessibilityServiceConnectionMonitorTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| AgentAccessibilityService unbinding & passive degradation | P4-ACC-002 | AUTOMATED_ROBOLECTRIC | `AccessibilityServiceConnectionMonitorTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Duplicate candidate rejection event suppression | P4-REJ-001 | AUTOMATED_ROBOLECTRIC | `AgentAccessibilityServiceTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Independent rejection logging for distinct packages | P4-REJ-002 | AUTOMATED_ROBOLECTRIC | `AgentAccessibilityServiceTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Deduplication state reset per request cycle | P4-REJ-003 | AUTOMATED_ROBOLECTRIC | `AgentAccessibilityServiceTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Valid external application classification & read-only guarantee | P4-REJ-004 | AUTOMATED_ROBOLECTRIC | `AgentAccessibilityServiceTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Physical phone Accessibility lifecycle procedure | P4-DEV-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 3.2 | Manual Lifecycle Test on Physical Phone | **NOT_RUN** |

---

## 7. Phase 5 — Observation & Snapshot Engine Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| SnapshotDiffEngine identical snapshot comparison | P5-DIFF-001 | AUTOMATED_JVM | `SnapshotDiffEngineTest.kt` | `./gradlew :core:test` | PASS |
| SnapshotDiffEngine added & removed node diffs | P5-DIFF-002 | AUTOMATED_JVM | `SnapshotDiffEngineTest.kt` | `./gradlew :core:test` | PASS |
| SnapshotDiffEngine changed attribute detection | P5-DIFF-003 | AUTOMATED_JVM | `SnapshotDiffEngineTest.kt` | `./gradlew :core:test` | PASS |
| SnapshotDiffEngine null & empty snapshot safety | P5-DIFF-004 | AUTOMATED_JVM | `SnapshotDiffEngineTest.kt` | `./gradlew :core:test` | PASS |
| Single-root retrieval & bounds extraction | P5-SNAP-001 | AUTOMATED_ROBOLECTRIC | `ObservationSnapshotExtractorPhase5Test.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Node identity & NodeIdentityConfidence assignment | P5-SNAP-002 | AUTOMATED_ROBOLECTRIC | `ObservationSnapshotExtractorPhase5Test.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Immediate AccessibilityNodeInfo recycling | P5-RECYC-001 | AUTOMATED_ROBOLECTRIC | `ObservationSnapshotExtractorPhase5Test.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Explicit STOP observation controls & state preservation | P5-CTRL-001 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Removal of direct Permission Center buttons | P5-UI-NAV-001 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Explicit Clear button resets screen presentation | P5-UI-CLR-001 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Consecutive Observe requests replace previous tree | P5-UI-DUP-001 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Node bounds rendering `[left,top,right,bottom] (WxH)` | P5-UI-BND-001 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Node rendering uniqueness & external snapshot isolation | P5-UI-DUP-005 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Read-only observation guarantee (0 dispatches) | P5-READ-001 | AUTOMATED_ROBOLECTRIC | `AgentAccessibilityServiceTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Physical phone Phase 5 single-root, stop control & heap footprint procedure | P5-DEV-SNAP-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 4 | Manual Observation & Memory Dump on Physical Phone | **NOT_RUN** |

---

## 8. Phase 6 — Universal Action Engine Target Resolution Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Self clickable node resolves to itself | P6-RESOLVE-001 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Non-clickable child TextView resolves to clickable parent MaterialButton | P6-RESOLVE-002 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Nearest valid clickable ancestor wins | P6-RESOLVE-003 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| No clickable ancestor returns NO_TARGET safely | P6-RESOLVE-004 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Long-clickable child & ancestor resolution | P6-RESOLVE-005 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Scrollable child resolves to scrollable ancestor | P6-RESOLVE-006 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Editable target resolution | P6-RESOLVE-007 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Identity confidence is preserved & derived correctly | P6-RESOLVE-008 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Resolution handles missing node ID safely | P6-RESOLVE-009 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Resolution never performs action execution | P6-RESOLVE-010 | AUTOMATED_JVM | `TargetResolverTest.kt` | `./gradlew :core:test` | PASS |
| Snapshot node re-acquires matching live node | P6-LIVE-001 | AUTOMATED_ROBOLECTRIC | `LiveTargetResolverTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Re-acquisition fails safely when target disappears | P6-LIVE-002 | AUTOMATED_ROBOLECTRIC | `LiveTargetResolverTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Re-acquisition rejects stale or mismatched identity | P6-LIVE-003 | AUTOMATED_ROBOLECTRIC | `LiveTargetResolverTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Re-acquired target capability verified on live node | P6-LIVE-004 | AUTOMATED_ROBOLECTRIC | `LiveTargetResolverTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Acquired AccessibilityNodeInfo objects correctly recycled | P6-LIVE-005 | AUTOMATED_ROBOLECTRIC | `LiveTargetResolverTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Zero action dispatch occurs during resolution and re-acquisition | P6-LIVE-006 | AUTOMATED_ROBOLECTRIC | `LiveTargetResolverTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Diagnostic logger passes eventId & deduplicates | P6-LOG-001 | AUTOMATED_ROBOLECTRIC | `TargetResolutionLoggerTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Physical phone Calculator child TextView -> MaterialButton parent resolution procedure | P6-DEV-TARGET-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 5 | Manual Target Resolution on Physical Phone | **NOT_RUN** |
