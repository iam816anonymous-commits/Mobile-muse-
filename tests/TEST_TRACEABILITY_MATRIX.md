# TEST_TRACEABILITY_MATRIX.md — Master Requirement to Test Traceability Matrix

## 1. Executive Summary

This matrix maps every requirement across frozen phases (Phase 0, Phase 1, Phase 2, Phase 3, Phase 4) to its exact stable Test ID, test classification type, executable source location, execution command, permissions, hardware, and verification status.

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
