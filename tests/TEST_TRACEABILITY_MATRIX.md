# TEST_TRACEABILITY_MATRIX.md — Master Requirement to Test Traceability Matrix

## 1. Executive Summary

This matrix maps every requirement across frozen phases (Phase 0, Phase 1, Phase 2, Phase 3.1, Phase 3.2) to its exact stable Test ID, test classification type, executable source location, execution command, permissions, hardware, and verification status.

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

## 5. Phase 3.1 — Accessibility Observation Foundation Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Observation snapshot & JSON serialization | P3-OBS-001 | AUTOMATED_JVM | `ObservationDomainModelsTest.kt` | `./gradlew :core:test` | PASS |
| Traversal bounds enforcement (`MAX_NODES=500`, `MAX_DEPTH=30`) | P3-OBS-002 | AUTOMATED_ROBOLECTRIC | `ObservationSnapshotExtractorTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Read-only observation guarantee (zero action execution) | P3-OBS-003 | AUTOMATED_ROBOLECTRIC | `AgentAccessibilityServiceTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Dedicated Current UI Observation screen | P3-UI-002 | AUTOMATED_ROBOLECTRIC | `CurrentObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Dedicated External App Observation screen | P3-UI-003 | AUTOMATED_ROBOLECTRIC | `ExternalObservationActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Physical phone local accessibility observation procedure | P3-DEV-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 3 | Manual Enablement in Android Settings | **NOT_RUN** |
| Physical phone external application observation procedure | P3-DEV-002 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 3 | Manual Launch of Chrome/Settings | **NOT_RUN** |
| Physical phone Chrome + Recents preservation procedure | P3.1-DEV-EXT-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 3.3 | Manual Chrome + Recents on Physical Phone | **NOT_RUN** |

---

## 6. Phase 3.2 — Observation Evidence Traceability

| Requirement | Test ID | Test Type | Executable Location | Execution Command | Status |
|---|---|---|---|---|---|
| Evidence model & SHA-256 provenance hash | P3.2-EVID-001 | AUTOMATED_JVM | `EvidenceDomainModelsTest.kt` | `./gradlew :core:test` | PASS |
| Dedicated Evidence UI Screen & Primitive Stream | P3.2-UI-001 | AUTOMATED_ROBOLECTRIC | `EvidenceActivityTest.kt` | `./gradlew :app:testDebugUnitTest` | PASS |
| Physical phone Observation Evidence Procedure | P3.2-DEV-EVID-001 | PHYSICAL_DEVICE | `PHYSICAL_DEVICE_TEST_PLAN.md` Sec 3.4 | Manual Evidence Capture on Physical Phone | **NOT_RUN** |
