# Phase 4 Test Cases — Accessibility Service Foundation & Deduplicated Candidate Rejection

## 1. Executive Summary
This document specifies the executable test suite for **Phase 4 — Accessibility Service Foundation** including the **External Candidate Rejection Log Deduplication Correction**.

All tests verify service lifecycle transitions (`BOUND`, `CONNECTING`, `UNBOUND`, `DEGRADED`), passive degradation logging, request-scoped candidate rejection deduplication, and read-only observation preservation without performing automated action execution or violating phase boundaries.

---

## 2. Test Cases Specification

### Test ID: P4-ACC-001
- **Requirement:** AgentAccessibilityService binding lifecycle monitoring
- **Purpose:** Verify that service connection notifies `AccessibilityServiceConnectionMonitor` and updates state to `BOUND`.
- **Preconditions:** LocalAgent application initialized.
- **Input:** `AgentAccessibilityService.onServiceConnected()` lifecycle trigger.
- **Expected Result:** Connection monitor state transitions to `BOUND`. Structured `AGENT_SERVICE_CONNECTED` event logged to `agent.db`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AccessibilityServiceConnectionMonitorTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE`
- **Hardware:** Baseline API 27+
- **Evidence:** `agent.db` event log entry `AGENT_SERVICE_CONNECTED` with `status: BOUND`.
- **Status:** PASS

### Test ID: P4-ACC-002
- **Requirement:** AgentAccessibilityService unbinding and passive degradation
- **Purpose:** Verify that service unbinding updates state to `DEGRADED` and logs disconnection without crashing.
- **Preconditions:** Service previously bound or active.
- **Input:** `AgentAccessibilityService.onUnbind()` or `onDestroy()` trigger.
- **Expected Result:** Connection monitor state updates to `DEGRADED`. Structured `AGENT_SERVICE_DISCONNECTED` event logged to `agent.db`. Diagnostic UI displays passive degradation banner. Zero crashes.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AccessibilityServiceConnectionMonitorTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `agent.db` event log entry `AGENT_SERVICE_DISCONNECTED` with `reason: ON_UNBIND`.
- **Status:** PASS

### Test ID: P4-REJ-001 (Test A)
- **Requirement:** Duplicate rejection event suppression within single observation request
- **Purpose:** Verify that evaluating duplicate invalid candidates (e.g. `com.android.systemui` multiple times) during 1 observation request emits exactly 1 `OBSERVATION_EXTERNAL_CANDIDATE_REJECTED` event.
- **Preconditions:** Active observation request cycle.
- **Input:** Multiple evaluations of `com.android.systemui` candidates.
- **Expected Result:** Exactly 1 `OBSERVATION_EXTERNAL_CANDIDATE_REJECTED` event emitted in `agent.db`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** DB query confirms exactly 1 rejection event for `com.android.systemui` during the request.
- **Status:** PASS

### Test ID: P4-REJ-002 (Test B)
- **Requirement:** Independent rejection logging for distinct candidate packages
- **Purpose:** Verify that distinct invalid candidates (e.g., `com.android.systemui` and `com.android.launcher3`) are each logged once during the same observation request.
- **Preconditions:** Active observation request containing `SystemUI` and `Launcher3`.
- **Input:** Mixed invalid window candidates.
- **Expected Result:** 1 rejection event for `SystemUI` + 1 rejection event for `Launcher3` (total 2 events).
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** DB query confirms 2 distinct rejection events with their respective package names.
- **Status:** PASS

### Test ID: P4-REJ-003 (Test C)
- **Requirement:** Deduplication state reset across distinct observation requests
- **Purpose:** Verify that a new observation request resets the deduplication set so invalid candidates are logged once per request cycle.
- **Preconditions:** Observation Request 1 completes; Observation Request 2 initiated.
- **Input:** `com.android.systemui` evaluated in Request 1 and Request 2.
- **Expected Result:** Total 2 rejection events logged (1 in Request 1, 1 in Request 2).
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** DB query confirms 2 events total across 2 requests.
- **Status:** PASS

### Test ID: P4-REJ-004 (Test D & E)
- **Requirement:** Valid external application classification & read-only guarantee
- **Purpose:** Verify that legitimate external applications (e.g., Calculator, Chrome, Settings) remain fully observable while strictly maintaining 0 action dispatches.
- **Preconditions:** Valid external package.
- **Input:** Candidate package `com.android.calculator2`.
- **Expected Result:** `isValidExternalApplicationPackage` returns `true`. Snapshot extracted. Action execution queue size = 0.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AgentAccessibilityServiceTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `isValidExternalApplicationPackage` returns `true` and queue size remains 0.
- **Status:** PASS
