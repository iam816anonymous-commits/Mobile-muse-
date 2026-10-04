# PHASE_01_TEST_CASES.md — Phase 1 Foundation & Domain Core Test Cases

## 1. Overview

This document specifies the test cases for Phase 1 (Foundation & Domain Core) of LocalAgent.

---

## 2. Test Cases

### P1-CMD-001
- **Requirement:** Command Normalizer Syntax Validation & Canonical Parsing
- **Purpose:** Proves `CommandNormalizer` correctly parses canonical command grammar (`back`, `home`, `recents`, `click <target>`, `long click <target>`, `scroll up/down`, `observe`, `status`, `launch <app>`).
- **Preconditions:** Pure Kotlin domain environment in `:core`.
- **Input:** Canonical input strings across all supported command types.
- **Expected Result:** `CommandParseResult.Success` with expected `NormalizedCommand` and `ActionType`.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `core/src/test/java/com/localagent/core/command/CommandNormalizerTest.kt` -> `testCanonicalCommandParsing()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.command.CommandNormalizerTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P1-CMD-002
- **Requirement:** Unknown Command Syntax Rejection at Normalizer Layer
- **Purpose:** Proves unrecognized input strings are rejected immediately as `CommandParseResult.UnknownCommand` before policy or queue dispatch.
- **Preconditions:** Pure Kotlin domain environment in `:core`.
- **Input:** Unrecognized strings (`"not real cmd"`, `"xyz 123"`).
- **Expected Result:** `CommandParseResult.UnknownCommand` with `rawInput` preserved.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `core/src/test/java/com/localagent/core/command/CommandNormalizerTest.kt` -> `testUnknownCommandReturnsUnknownCommand()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.command.CommandNormalizerTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P1-CMD-003
- **Requirement:** Empty / Whitespace Input Rejection
- **Purpose:** Proves empty or whitespace-only inputs return `CommandParseResult.InvalidInput`.
- **Preconditions:** Pure Kotlin domain environment in `:core`.
- **Input:** Empty string `""` or whitespace `"   "`.
- **Expected Result:** `CommandParseResult.InvalidInput`.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `core/src/test/java/com/localagent/core/command/CommandNormalizerTest.kt` -> `testEmptyInputReturnsInvalidInput()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.command.CommandNormalizerTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P1-POL-001
- **Requirement:** Action Policy Engine Risk Tier Evaluation
- **Purpose:** Proves `ActionPolicyEngine` classifies actions into `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` risk levels and requires confirmation for high-risk commands.
- **Preconditions:** `CapabilityRegistry` initialized with rule descriptors.
- **Input:** `NormalizedCommand` for `APP_LAUNCH` (HIGH risk) vs `UI_CLICK` (LOW risk).
- **Expected Result:** `HIGH` risk requires confirmation; `LOW` risk evaluates to direct dispatch.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `core/src/test/java/com/localagent/core/policy/ActionPolicyEngineTest.kt` -> `testHighRiskActionRequiresConfirmation()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.policy.ActionPolicyEngineTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P1-QUEUE-001
- **Requirement:** Goal Dispatcher Priority Queueing & Concurrency Lock
- **Purpose:** Proves `GoalDispatcher` orders commands by priority and enforces atomic channel locking via `ExecutionLock`.
- **Preconditions:** `GoalDispatcher` instantiated.
- **Input:** Enqueue Priority 1 and Priority 3 commands simultaneously.
- **Expected Result:** Priority 1 command polled first; secondary polling blocks until active execution completes.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `core/src/test/java/com/localagent/core/execution/GoalDispatcherTest.kt` -> `testPriorityQueueAndExecutionLock()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.execution.GoalDispatcherTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS

### P1-LIFE-001
- **Requirement:** Task Lifecycle State Machine & Low Memory Killer (LMK) Transitions
- **Purpose:** Proves task state machine manages transitions and supports LMK recovery (`INTERRUPTED` -> `RECOVERING` -> `QUEUED`).
- **Preconditions:** `TaskLifecycle` instantiated.
- **Input:** Task interrupted during execution state.
- **Expected Result:** State transitions legally to `INTERRUPTED` and resumes via `RECOVERING`.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `core/src/test/java/com/localagent/core/lifecycle/TaskLifecycleTest.kt` -> `testLmkRecoveryTransition()`
- **Execution Command:** `./gradlew :core:test --tests "com.localagent.core.lifecycle.TaskLifecycleTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`core/build/test-results/test/`)
- **Status:** PASS
